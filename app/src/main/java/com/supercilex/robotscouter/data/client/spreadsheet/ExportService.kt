package com.supercilex.robotscouter.data.client.spreadsheet

import android.Manifest
import android.app.IntentService
import android.content.Intent
import android.support.annotation.RequiresPermission
import android.support.annotation.Size
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.Tasks
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.getTeamListExtra
import com.supercilex.robotscouter.util.data.model.getScouts
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.putExtra
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logExportTeamsEvent
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
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

        if (isOffline()) showToast(getString(R.string.export_offline_rationale))

        val teams = intent.getTeamListExtra()
        try {
            val scoutTasks = teams.map { it.getScouts() }
            Tasks.await(Tasks.whenAll(scoutTasks), TIMEOUT, TimeUnit.MINUTES)
            onHandleScouts(
                    notificationManager,
                    scoutTasks.withIndex().associate { teams[it.index] to it.value.result }
            )
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
            async {
                SpreadsheetExporter(scouts, notificationManager, templateNames[templateId]!!)
                        .export()
            }.addOnFailureListener {
                abortCritical(it, notificationManager)
            }
        }), TIMEOUT, TimeUnit.MINUTES)
    }

    private fun getTemplateNames(templateIds: Set<String>): Map<String, String> {
        val unknownTemplateName: String = getString(R.string.export_unknown_template_title)

        val allPossibleTemplateNames: Map<String, String> = Tasks.await(
                getTemplatesQuery().get()
        ).associate {
            val scout = scoutParser.parseSnapshot(it)
            scout.id to (scout.name ?: unknownTemplateName)
        }.toMutableMap().apply {
            putAll(TemplateType.values.associate {
                it.id.toString() to resources.getStringArray(R.array.template_new_options)[it.id]
            })
        }

        val usedTemplates = HashMap<String, Int>()
        return templateIds.associate {
            // Getting the name will be null if the user deletes a template
            it to (allPossibleTemplateNames[it] ?: unknownTemplateName)
        }.mapValues { (_, name) ->
            usedTemplates[name]?.let {
                usedTemplates[name] = it + 1
                "$name ($it)"
            } ?: run {
                usedTemplates.put(name, 1)
                name
            }
        }
    }

    private fun zipScouts(map: Map<Team, List<Scout>>): Map<String, Map<Team, List<Scout>>> {
        fun initMap(map: HashMap<Team, ArrayList<Scout>>, team: Team, scout: Scout) =
                map.put(team, ArrayList<Scout>().also { it += scout })

        val zippedScouts = HashMap<String, HashMap<Team, ArrayList<Scout>>>()
        for ((team, scouts) in map) {
            for (scout in scouts) {
                zippedScouts[scout.templateId]?.also {
                    it[team]?.also { it += scout } ?: initMap(it, team, scout)
                } ?: zippedScouts.put(scout.templateId, HashMap<Team, ArrayList<Scout>>()
                        .also { initMap(it, team, scout) })
            }
        }
        return zippedScouts
    }

    private fun abortCritical(e: Exception, notificationManager: ExportNotificationManager) {
        FirebaseCrash.report(e)
        Crashlytics.logException(e)
        showToast("${getString(R.string.fui_general_error)}\n\n${e.message}")
        async { notificationManager.abort() }.logFailures()
    }

    companion object {
        private const val TAG = "ExportService"
        private const val TIMEOUT = 10L

        /** @return true if an export was attempted, false otherwise */
        fun exportAndShareSpreadSheet(fragment: Fragment,
                                      permHandler: PermissionRequestHandler,
                                      @Size(min = 1) teams: List<Team>): Boolean {
            if (teams.isEmpty()) return false

            val context = fragment.context!!

            if (!EasyPermissions.hasPermissions(context, *permHandler.permsArray)) {
                permHandler.requestPerms(R.string.export_write_storage_rationale)
                return false
            }

            snackbar(fragment.view!!, R.string.export_progress_hint)

            logExportTeamsEvent(teams)
            ContextCompat.startForegroundService(
                    context,
                    context.intentFor<ExportService>().putExtra(teams)
            )

            return true
        }
    }
}
