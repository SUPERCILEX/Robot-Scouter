package com.supercilex.robotscouter.data.client.spreadsheet

import android.support.v4.app.NotificationCompat
import android.support.v4.app.ServiceCompat
import android.support.v4.content.ContextCompat
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.model.getNames
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.ui.EXPORT_IN_PROGRESS_CHANNEL
import com.supercilex.robotscouter.util.ui.FilteringNotificationManager
import com.supercilex.robotscouter.util.ui.notificationManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class ExportNotificationManager(private val service: ExportService) {
    private val notificationFilter = FilteringNotificationManager()

    private val masterNotification: NotificationCompat.Builder
        get() = NotificationCompat.Builder(RobotScouter, EXPORT_IN_PROGRESS_CHANNEL)
                .setGroup(hashCode().toString())
                .setGroupSummary(true)
                .setContentTitle(RobotScouter.getString(R.string.export_overall_progress_title))
                .setColor(ContextCompat.getColor(RobotScouter, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
    private val exportNotification: NotificationCompat.Builder
        get() = NotificationCompat.Builder(RobotScouter, EXPORT_IN_PROGRESS_CHANNEL)
                .setGroup(hashCode().toString())
                .setContentTitle(RobotScouter.getString(R.string.export_progress_title))
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setColor(ContextCompat.getColor(RobotScouter, R.color.colorPrimary))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

    private val masterNotificationHolder = MasterNotificationHolder()
    private val exporters = ConcurrentHashMap<SpreadsheetExporter, NotificationHolder>()

    private var nLoadingChunks: Int by LateinitVal()
    private var nTemplates: Int by LateinitVal()
    private var teams: Set<Team> by LateinitVal()
    private var pendingTaskCount: Int by Delegates.notNull()

    init {
        service.startForeground(hashCode(), masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(RobotScouter.getString(R.string.export_load_status))
                .updateProgress(ESTIMATED_MASTER_OPS, 0)
                .build())
        notificationFilter.start()
    }

    fun startLoading(chunks: Int) {
        nLoadingChunks = chunks
        masterNotificationHolder.maxProgress = EXTRA_MASTER_OPS + nLoadingChunks

        notificationFilter.notify(hashCode(), masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(RobotScouter.getString(R.string.export_load_status))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())
    }

    fun updateLoadProgress() {
        masterNotificationHolder.progress++
    }

    fun loading(teams: List<Team>) {
        notificationFilter.notify(hashCode(), masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(RobotScouter.getString(
                        R.string.export_load_status_detail,
                        teams.first().number,
                        teams.last().number
                ))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())
    }

    fun setData(nTemplates: Int, teams: Set<Team>) {
        this.nTemplates = nTemplates
        this.teams = teams
        pendingTaskCount = nTemplates

        masterNotificationHolder.progress = EXTRA_MASTER_OPS + nLoadingChunks
        masterNotificationHolder.maxProgress = EXTRA_MASTER_OPS + nLoadingChunks + nTemplates

        notificationFilter.notify(hashCode(), masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(RobotScouter.getString(R.string.export_load_status))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())
    }

    @Synchronized
    fun addExporter(exporter: SpreadsheetExporter): Int {
        check(exporters.size < pendingTaskCount) { "More exporters than templates" }

        val id = exporter.hashCode()
        val teams = exporter.scouts.keys
        val maxProgress = teams.size +
                if (teams.isSingleton) EXTRA_EXPORT_OPS_SINGLE else EXTRA_EXPORT_OPS_POLY

        notificationFilter.notify(id, exportNotification
                .setContentText(RobotScouter.getString(R.string.export_initialize_status))
                .updateProgress(maxProgress, 0)
                .build())
        if (pendingTaskCount == nTemplates && exporters.isEmpty()) {
            notificationFilter.notify(hashCode(), masterNotification
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentText(RobotScouter.getString(
                            R.string.export_template_status, exporter.templateName))
                    .updateProgress(
                            masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                    .build())
        }

        exporters[exporter] = NotificationHolder(id, maxProgress)

        return id
    }

    fun updateProgress(exporter: SpreadsheetExporter, team: Team) =
            next(exporter, exportNotification.setContentText(team.toString()))

    fun onStartBuildingAverageSheet(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.getString(R.string.export_average_status)))
    }

    fun onStartCleanup(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.getString(R.string.export_cleanup_status)))
    }

    @Synchronized
    fun removeExporter(exporter: SpreadsheetExporter, notification: NotificationCompat.Builder) {
        val (id) = exporters.remove(exporter)!!
        // The original notification must be cancelled because we're changing channels
        notificationManager.cancel(id)
        notificationFilter.notify(id, notification.setGroup(hashCode().toString()).build(), true)

        if (--pendingTaskCount == 0) {
            notificationFilter.notify(hashCode(), masterNotification
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentText(RobotScouter.resources.getQuantityString(
                            R.plurals.export_complete_message, teams.size, teams.getNames()))
                    .setSubText(RobotScouter.resources.getQuantityString(
                            R.plurals.export_complete_subtitle, nTemplates, nTemplates))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build(), true)
            notificationFilter.stop()
            ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_DETACH)
        } else {
            notificationFilter.notify(hashCode(), masterNotification
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentText(RobotScouter.getString(
                            R.string.export_template_status,
                            exporters.keys.first().templateName))
                    .updateProgress(
                            masterNotificationHolder.maxProgress,
                            ++masterNotificationHolder.progress)
                    .build())
        }
    }

    fun abort() {
        notificationFilter.stopNow()
        for ((_, holder) in exporters) notificationManager.cancel(holder.id)
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun next(exporter: SpreadsheetExporter, notification: NotificationCompat.Builder) {
        val holder = exporters[exporter]!!
        notificationFilter.notify(holder.id, notification
                .updateProgress(holder.maxProgress, ++holder.progress)
                .build())
    }

    private fun NotificationCompat.Builder.updateProgress(
            maxProgress: Int, progress: Int): NotificationCompat.Builder {
        require(progress >= 0) { "Progress must be greater than 0" }
        require(maxProgress >= progress) {
            "Max progress ($maxProgress) must be greater or equal to progress ($progress)"
        }

        val percentage = (progress.toFloat() / maxProgress.toFloat() * 100).roundToInt()
        setProgress(maxProgress, progress, false).setSubText("$percentage%")
        return this
    }

    private data class MasterNotificationHolder(var progress: Int = 0) {
        var maxProgress: Int by Delegates.notNull()
    }

    private data class NotificationHolder(val id: Int, val maxProgress: Int, var progress: Int = 0)

    private companion object {
        /** Accounts for a load step. */
        const val EXTRA_MASTER_OPS = 1
        /** Accounts for two export files. */
        const val ESTIMATED_MASTER_OPS = EXTRA_MASTER_OPS + 2

        /** Accounts one cleanup step. */
        const val EXTRA_EXPORT_OPS_SINGLE = 1
        /** Accounts for the average sheet step. */
        const val EXTRA_EXPORT_OPS_POLY = EXTRA_EXPORT_OPS_SINGLE + 1
    }
}
