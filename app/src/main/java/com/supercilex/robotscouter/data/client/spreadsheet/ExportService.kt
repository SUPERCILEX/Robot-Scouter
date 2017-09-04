package com.supercilex.robotscouter.data.client.spreadsheet

import android.Manifest
import android.app.IntentService
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.support.annotation.RequiresPermission
import android.support.annotation.Size
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.isNativeTemplateType
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.TemplateNamesLiveData
import com.supercilex.robotscouter.util.data.getTeamListExtra
import com.supercilex.robotscouter.util.data.model.Scouts
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.putExtra
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logExportTeamsEvent
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import org.apache.poi.ss.formula.WorkbookEvaluator
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

class ExportService : IntentService(TAG) {
    init {
        setIntentRedelivery(true)
    }

    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    override fun onHandleIntent(intent: Intent) {
        val notificationManager = ExportNotificationManager(this)

        if (isOffline()) showToast(getString(R.string.export_warning_offline))

        val teams = intent.getTeamListExtra()
        try {
            // Force a refresh
            Tasks.await(Scouts.getAll(teams), 5, TimeUnit.MINUTES)

            notificationManager.onStartSecondLoad()

            val scouts = Tasks.await(Scouts.getAll(teams), 5, TimeUnit.MINUTES)

            if (scouts.size != teams.size) {
                // Some error occurred, let's try again
                startService(Intent(this, ExportService::class.java).putExtra(teams))
                return
            }

            onHandleScouts(notificationManager, scouts)
        } catch (e: Exception) {
            abortCritical(e, notificationManager)
        }
    }

    private fun onHandleScouts(notificationManager: ExportNotificationManager,
                               newScouts: Map<Team, List<Scout>>) {
        val zippedScouts = zipScouts(newScouts)

        notificationManager.setNumOfTemplates(zippedScouts.size)

        val keepAliveListener = object : ChangeEventListenerBase {}
        val array = TemplateNamesLiveData.value!!
        val namesListener =
                MutableLiveData<ObservableSnapshotArray<String>>().also { it.postValue(array) }

        array.addChangeEventListener(keepAliveListener)
        Tasks.await(Tasks.whenAll(zippedScouts.map { (templateKey, scouts) ->
            namesListener.observeOnDataChanged().observeOnce {
                async {
                    SpreadsheetExporter(scouts, notificationManager, fun(): String {
                        if (isNativeTemplateType(templateKey)) {
                            return resources.getStringArray(
                                    R.array.new_template_options)[templateKey.toInt()]
                        }

                        for ((index, snapshot) in it.withIndex()) {
                            if (snapshot.key == templateKey) return it.getObject(index)
                        }

                        return getString(R.string.title_unknown_template)
                    }.invoke()).export()
                }
            }
        }).addOnCompleteListener { array.removeChangeEventListener(keepAliveListener) })
    }

    private fun zipScouts(map: Map<Team, List<Scout>>): Map<String, Map<Team, List<Scout>>> {
        fun initMap(map: HashMap<Team, ArrayList<Scout>>, team: Team, scout: Scout) =
                map.put(team, ArrayList<Scout>().also { it.add(scout) })

        val zippedScouts = HashMap<String, HashMap<Team, ArrayList<Scout>>>()
        for ((team, scouts) in map) {
            for (scout in scouts) {
                zippedScouts[scout.templateKey]?.also {
                    it[team]?.also { it += scout } ?: initMap(it, team, scout)
                } ?: zippedScouts.put(scout.templateKey, HashMap<Team, ArrayList<Scout>>()
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
                permHandler.requestPerms(R.string.write_storage_rationale_spreadsheet)
                return false
            }

            Snackbar.make(fragment.view!!, R.string.export_hint, Snackbar.LENGTH_SHORT).show()

            logExportTeamsEvent(teams)
            ContextCompat.startForegroundService(
                    fragment.activity,
                    Intent(context, ExportService::class.java).putExtra(teams))

            return true
        }
    }
}
