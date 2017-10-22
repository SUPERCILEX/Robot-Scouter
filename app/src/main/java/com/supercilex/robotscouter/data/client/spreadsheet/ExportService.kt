package com.supercilex.robotscouter.data.client.spreadsheet

import android.Manifest
import android.app.IntentService
import android.content.Intent
import android.support.annotation.RequiresPermission
import android.support.annotation.Size
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.SCOUT_PARSER
import com.supercilex.robotscouter.util.data.getTeamListExtra
import com.supercilex.robotscouter.util.data.model.getScouts
import com.supercilex.robotscouter.util.data.model.getTemplateName
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.putExtra
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logExportTeamsEvent
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import org.apache.poi.ss.formula.WorkbookEvaluator
import pub.devrel.easypermissions.EasyPermissions

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
            Tasks.await(Tasks.whenAll(scoutTasks))
            onHandleScouts(
                    notificationManager,
                    scoutTasks.withIndex().associate { teams[it.index] to it.value.result })
        } catch (e: Exception) {
            abortCritical(e, notificationManager)
        }
    }

    private fun onHandleScouts(notificationManager: ExportNotificationManager,
                               newScouts: Map<Team, List<Scout>>) {
        val zippedScouts = zipScouts(newScouts)

        notificationManager.setNumOfTemplates(zippedScouts.size)

        val templates = Tasks.await(getTemplatesQuery().get())
                .map { SCOUT_PARSER.parseSnapshot(it) }
        Tasks.await(Tasks.whenAll(zippedScouts.map { (templateId, scouts) ->
            async {
                TemplateType.coerce(templateId)?.let {
                    SpreadsheetExporter(scouts, notificationManager, resources.getStringArray(
                            R.array.template_new_options)[it.id]).export()
                } ?: run {
                    SpreadsheetExporter(scouts, notificationManager, {
                        templates.find { it.id == templateId }?.let {
                            it.getTemplateName(templates.indexOf(it))
                        } ?: getString(R.string.export_unknown_template_title)
                    }.invoke()).export()
                }
            }
        }))
    }

    private fun zipScouts(map: Map<Team, List<Scout>>): Map<String, Map<Team, List<Scout>>> {
        fun initMap(map: HashMap<Team, ArrayList<Scout>>, team: Team, scout: Scout) =
                map.put(team, ArrayList<Scout>().also { it.add(scout) })

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

    companion object {
        private const val TAG = "ExportService"

        init {
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLInputFactory",
                    "com.fasterxml.aalto.stax.InputFactoryImpl")
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                    "com.fasterxml.aalto.stax.OutputFactoryImpl")
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLEventFactory",
                    "com.fasterxml.aalto.stax.EventFactoryImpl")
            WorkbookEvaluator.registerFunction("AVERAGEIF", AVERAGEIF_FUNCTION)
        }

        /** @return true if an export was attempted, false otherwise */
        fun exportAndShareSpreadSheet(fragment: Fragment,
                                      permHandler: PermissionRequestHandler,
                                      @Size(min = 1) teams: List<Team>): Boolean {
            if (teams.isEmpty()) return false

            val context = fragment.context

            if (!EasyPermissions.hasPermissions(context, *permHandler.permsArray)) {
                permHandler.requestPerms(R.string.export_write_storage_rationale)
                return false
            }

            Snackbar.make(fragment.view!!, R.string.export_progress_hint, Snackbar.LENGTH_SHORT)
                    .show()

            logExportTeamsEvent(teams)
            ContextCompat.startForegroundService(
                    fragment.activity,
                    Intent(context, ExportService::class.java).putExtra(teams))

            return true
        }
    }
}
