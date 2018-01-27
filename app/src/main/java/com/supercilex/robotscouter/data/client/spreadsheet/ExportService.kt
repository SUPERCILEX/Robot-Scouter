package com.supercilex.robotscouter.data.client.spreadsheet

import android.Manifest
import android.app.IntentService
import android.content.Intent
import android.support.annotation.RequiresPermission
import android.support.annotation.Size
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.data.getTeamListExtra
import com.supercilex.robotscouter.util.data.model.getScouts
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.putExtra
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.data.shouldShowRatingDialog
import com.supercilex.robotscouter.util.doAsync
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.isOnline
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logExport
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import com.supercilex.robotscouter.util.ui.RatingDialog
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.intentFor
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

class ExportService : IntentService(TAG) {
    init {
        setIntentRedelivery(true)
    }

    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onHandleIntent(intent: Intent) {
        val notificationManager = ExportNotificationManager(this)

        if (isOffline) showToast(getString(R.string.export_offline_rationale))

        val teams: List<Team> = intent.getTeamListExtra().toMutableList().apply { sort() }
        val chunks = teams.chunked(SYNCHRONOUS_QUERY_CHUNK)
        notificationManager.startLoading(chunks.size)

        try {
            onHandleScouts(notificationManager, chunks.map {
                notificationManager.loading(it)
                Tasks.await(
                        Tasks.whenAllSuccess<List<Scout>>(it.map { it.getScouts() }),
                        TIMEOUT,
                        TimeUnit.MINUTES
                ).also { notificationManager.updateLoadProgress() }
            }.flatten().withIndex().associate {
                teams[it.index] to it.value
            })
        } catch (e: Exception) {
            abortCritical(e, notificationManager)
        }
    }

    private fun onHandleScouts(
            notificationManager: ExportNotificationManager,
            newScouts: Map<Team, List<Scout>>
    ) {
        val zippedScouts = zipScouts(newScouts)

        notificationManager.setData(zippedScouts.size, newScouts.keys)

        val templateNames = getTemplateNames(zippedScouts.keys)
        Tasks.await(Tasks.whenAll(zippedScouts.map { (templateId, scouts) ->
            doAsync {
                if (!notificationManager.isStopped()) {
                    SpreadsheetExporter(scouts, notificationManager, templateNames[templateId]!!)
                            .export()
                }
            }.addOnFailureListener(AsyncTaskExecutor, OnFailureListener {
                abortCritical(it, notificationManager)
            })
        }), TIMEOUT, TimeUnit.MINUTES)
    }

    private fun getTemplateNames(templateIds: Set<String>): Map<String, String> {
        val unknownTemplateName: String = getString(R.string.export_unknown_template_title)

        val allPossibleTemplateNames: Map<String, String> = Tasks.await(
                getTemplatesQuery().log().get()
        ).associate {
            val scout = scoutParser.parseSnapshot(it)
            scout.id to (scout.name ?: unknownTemplateName)
        }.toMutableMap().apply {
            putAll(TemplateType.values.associate {
                it.id.toString() to resources.getStringArray(R.array.template_new_options)[it.id]
            })
        }

        val usedTemplates = mutableMapOf<String, Int>()
        return templateIds.associate {
            // Getting the name will be null if the user deletes a template
            it to (allPossibleTemplateNames[it] ?: unknownTemplateName)
        }.mapValues { (_, name) ->
            usedTemplates[name]?.let {
                usedTemplates[name] = it + 1
                "$name ($it)"
            } ?: run {
                usedTemplates[name] = 1
                name
            }
        }
    }

    private fun zipScouts(map: Map<Team, List<Scout>>): Map<String, Map<Team, List<Scout>>> {
        val zippedScouts = mutableMapOf<String, MutableMap<Team, MutableList<Scout>>>()
        for ((team, scouts) in map) {
            for (scout in scouts) {
                zippedScouts[scout.templateId]?.also {
                    it[team]?.also { it += scout } ?: it.put(team, mutableListOf(scout))
                } ?: zippedScouts.put(scout.templateId, mutableMapOf(team to mutableListOf(scout)))
            }
        }
        return zippedScouts
    }

    private fun abortCritical(e: Exception, notificationManager: ExportNotificationManager) {
        CrashLogger.onFailure(e)
        showToast("${getString(R.string.fui_general_error)}\n\n${e.message}")
        notificationManager.abort()
    }

    companion object {
        private const val TAG = "ExportService"
        private const val SYNCHRONOUS_QUERY_CHUNK = 10
        private const val TIMEOUT = 10L

        private const val MIN_TEAMS_TO_RATE = 10

        /** @return true if an export was attempted, false otherwise */
        fun exportAndShareSpreadSheet(fragment: Fragment,
                                      permHandler: PermissionRequestHandler,
                                      @Size(min = 1) mutableTeams: List<Team>): Boolean {
            val teams = mutableTeams.toList()
            if (teams.isEmpty()) return false

            val context = fragment.context!!

            if (!EasyPermissions.hasPermissions(context, *permHandler.perms.toTypedArray())) {
                permHandler.requestPerms(fragment, R.string.export_write_storage_rationale)
                return false
            }

            snackbar(fragment.view!!, R.string.export_progress_hint)

            teams.logExport()
            ContextCompat.startForegroundService(
                    context,
                    context.intentFor<ExportService>().putExtra(teams)
            )

            if (teams.size >= MIN_TEAMS_TO_RATE && isOnline) {
                val f = fragment.asReference()
                launch(UI) {
                    async { fetchAndActivate() }.await()
                    if (shouldShowRatingDialog) RatingDialog.show(f().childFragmentManager)
                }
            }

            return true
        }
    }
}
