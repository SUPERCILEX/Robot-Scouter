package com.supercilex.robotscouter.data.client.spreadsheet

import android.Manifest
import android.app.IntentService
import android.content.Intent
import android.support.annotation.RequiresPermission
import android.support.annotation.Size
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.asLifecycleReference
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.exportsFolder
import com.supercilex.robotscouter.util.data.getTeamListExtra
import com.supercilex.robotscouter.util.data.model.getScouts
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.putExtra
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.data.shouldShowRatingDialog
import com.supercilex.robotscouter.util.fetchAndActivate
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.isOnline
import com.supercilex.robotscouter.util.logExport
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import com.supercilex.robotscouter.util.ui.RatingDialog
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.intentFor
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
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

                runBlocking {
                    withTimeout(TIMEOUT, TimeUnit.MINUTES) {
                        it.map { async { it.getScouts() } }.await()
                    }
                }.also { notificationManager.updateLoadProgress() }
            }.flatten().withIndex().associate {
                teams[it.index] to it.value
            })
        } catch (e: Exception) {
            notificationManager.abortCritical(e)
        }
    }

    private fun onHandleScouts(
            notificationManager: ExportNotificationManager,
            newScouts: Map<Team, List<Scout>>
    ) {
        if (newScouts.values.all { it.isEmpty() }) {
            notificationManager.setData(0, newScouts.keys)
            notificationManager.stop()
            return
        }

        val zippedScouts = zipScouts(newScouts)

        notificationManager.setData(zippedScouts.size, newScouts.keys)

        val exportFolder = File(checkNotNull(exportsFolder) {
            "Couldn't get write access"
        }, "Robot Scouter export_${System.currentTimeMillis()}")

        runBlocking {
            val templateNames = getTemplateNames(zippedScouts.keys)
            withTimeout(TIMEOUT, TimeUnit.MINUTES) {
                zippedScouts.map { (templateId, scouts) ->
                    async {
                        if (!notificationManager.isStopped()) {
                            try {
                                TemplateExporter(
                                        scouts,
                                        notificationManager,
                                        exportFolder,
                                        templateNames[templateId]
                                ).export()
                            } catch (e: Exception) {
                                notificationManager.abortCritical(e)
                                throw CancellationException()
                            }
                        }
                    }
                }.await()
            }
        }
    }

    private suspend fun getTemplateNames(templateIds: Set<String>): Map<String, String?> {
        val unknownTemplateName: String = getString(R.string.export_unknown_template_title)

        val templatesSnapshot: List<DocumentSnapshot> = try {
            getTemplatesQuery().get().await().documents
        } catch (e: Exception) {
            CrashLogger.onFailure(e)
            emptyList()
        }
        val allPossibleTemplateNames: Map<String, String?> = templatesSnapshot.associate {
            scoutParser.parseSnapshot(it).let { it.id to it.name }
        }.toMutableMap().apply {
            putAll(TemplateType.values.associate {
                it.id.toString() to resources.getStringArray(R.array.template_new_options)[it.id]
            })
        }

        val usedTemplates = mutableMapOf<String, Int>()
        return templateIds.associate {
            if (allPossibleTemplateNames.contains(it)) {
                it to allPossibleTemplateNames[it]
            } else {
                // User deleted template
                it to unknownTemplateName
            }
        }.mapValues { (_, name) ->
            if (name == null) return@mapValues null
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
                zippedScouts.getOrPut(scout.templateId) {
                    mutableMapOf()
                }.getOrPut(team) {
                    mutableListOf()
                }.add(scout)
            }
        }
        return zippedScouts
    }

    private fun ExportNotificationManager.abortCritical(e: Exception) {
        if (e !is TimeoutCancellationException) CrashLogger.onFailure(e)
        abort()
        if (e !is CancellationException) showToast("${getString(R.string.fui_error_unknown)}\n\n$e")
    }

    companion object {
        private const val TAG = "ExportService"
        private const val SYNCHRONOUS_QUERY_CHUNK = 10
        private const val TIMEOUT = 10L

        private const val MIN_TEAMS_TO_RATE = 10

        /** @return true if an export was attempted, false otherwise */
        fun exportAndShareSpreadSheet(
                fragment: Fragment,
                permHandler: PermissionRequestHandler,
                @Size(min = 1) mutableTeams: List<Team>
        ): Boolean {
            val teams = mutableTeams.toList()
            if (teams.isEmpty()) return false

            val context = fragment.requireContext()

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
                val f = fragment.asLifecycleReference()
                async(UI) {
                    async { fetchAndActivate() }.await()
                    if (shouldShowRatingDialog) RatingDialog.show(f().childFragmentManager)
                }.logFailures()
            }

            return true
        }
    }
}
