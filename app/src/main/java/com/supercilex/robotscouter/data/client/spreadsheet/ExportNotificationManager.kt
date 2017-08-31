package com.supercilex.robotscouter.data.client.spreadsheet

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.app.ServiceCompat
import android.support.v4.content.ContextCompat
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.ui.EXPORT_IN_PROGRESS_CHANNEL
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates

class ExportNotificationManager(private val service: ExportService) {
    private val manager =
            RobotScouter.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val masterNotification: NotificationCompat.Builder = NotificationCompat.Builder(
            RobotScouter.INSTANCE, EXPORT_IN_PROGRESS_CHANNEL)
            .setGroup(hashCode().toString())
            .setGroupSummary(true)
            .setContentTitle(RobotScouter.INSTANCE.getString(R.string.export_title))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setColor(ContextCompat.getColor(RobotScouter.INSTANCE, R.color.colorPrimary))
            .updateProgress(ESTIMATED_MASTER_OPS, 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    private val exportNotification: NotificationCompat.Builder = NotificationCompat.Builder(
            RobotScouter.INSTANCE, EXPORT_IN_PROGRESS_CHANNEL)
            .setGroup(hashCode().toString())
            .setContentTitle(RobotScouter.INSTANCE.getString(R.string.export_in_progress_title))
            .setContentText(RobotScouter.INSTANCE.getString(R.string.export_initializing_title))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setColor(ContextCompat.getColor(RobotScouter.INSTANCE, R.color.colorPrimary))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private val masterNotificationHolder = MasterNotificationHolder()
    private val exporters = ConcurrentHashMap<SpreadsheetExporter, NotificationHolder>()
    private var nTemplates: Int by Delegates.notNull()

    init {
        service.startForeground(
                hashCode(),
                masterNotification
                        .setContentText(RobotScouter.INSTANCE.getString(R.string.exporting_status_loading))
                        .build())
    }

    fun onStartSecondLoad() {
        masterNotificationHolder.progress = 1
        manager.notify(hashCode(), masterNotification
                .updateProgress(ESTIMATED_MASTER_OPS, masterNotificationHolder.progress)
                .build())
    }

    fun setNumOfTemplates(nTemplates: Int) {
        this.nTemplates = nTemplates
        masterNotificationHolder.progress = EXTRA_MASTER_OPS
        masterNotificationHolder.maxProgress = EXTRA_MASTER_OPS + nTemplates
        masterNotification.updateProgress(
                masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
    }

    fun addExporter(exporter: SpreadsheetExporter): Int {
        val id = exporter.hashCode()
        val teams = exporter.scouts.keys
        val maxProgress = teams.size +
                if (teams.size == SINGLE_ITEM) EXTRA_EXPORT_OPS_SINGLE else EXTRA_EXPORT_OPS_POLY

        val notification = exportNotification.updateProgress(maxProgress, 0)
        exporters[exporter] = NotificationHolder(id, notification, maxProgress)

        manager.notify(id, notification.build())
        manager.notify(hashCode(), masterNotification
                .setContentText(RobotScouter.INSTANCE.getString(
                        R.string.exporting_status_template, exporter.templateName))
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())

        return id
    }

    fun updateProgress(exporter: SpreadsheetExporter, team: Team) {
        next(exporter, exportNotification.setContentText(team.toString()).build())
    }

    fun onStartBuildingAverageSheet(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.INSTANCE.getString(R.string.exporting_status_average))
                .build())
    }

    fun onStartCleanup(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.INSTANCE.getString(R.string.exporting_status_cleanup))
                .build())
    }

    fun removeExporter(exporter: SpreadsheetExporter, notification: NotificationCompat.Builder) {
        val (id) = exporters.remove(exporter)!!
        manager.notify(id, notification.setGroup(hashCode().toString()).build())

        if (exporters.isEmpty()) {
            manager.notify(hashCode(), masterNotification
                    .setSmallIcon(R.drawable.ic_logo)
                    .setSubText(RobotScouter.INSTANCE.resources.getQuantityString(
                            R.plurals.export_complete_subtitle, nTemplates, nTemplates))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build())
            ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_DETACH)
        } else {
            manager.notify(hashCode(), masterNotification
                    .setContentText(RobotScouter.INSTANCE.getString(
                            R.string.exporting_status_template,
                            exporters.keys.first().templateName))
                    .updateProgress(
                            masterNotificationHolder.maxProgress,
                            ++masterNotificationHolder.progress)
                    .build())
        }
    }

    fun abort() {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
        manager.cancel(hashCode())
        for (exporter in exporters) manager.cancel(exporter.value.id)
    }

    private fun next(exporter: SpreadsheetExporter, notification: Notification) {
        val holder = exporters[exporter]!!
        incrementProgress(holder)
        manager.notify(holder.id, notification)
    }

    private fun incrementProgress(holder: NotificationHolder) {
        exportNotification.updateProgress(holder.maxProgress, ++holder.progress)
    }

    private fun NotificationCompat.Builder.updateProgress(
            maxProgress: Int, progress: Int): NotificationCompat.Builder {
        if (progress < 0) throw IllegalArgumentException("Progress must be greater than 0")
        if (maxProgress < progress) throw IllegalArgumentException(
                "Max progress ($maxProgress) must be greater or equal to progress ($progress)")

        val percentage = Math.round(progress.toFloat() / maxProgress.toFloat() * 100)
        setProgress(maxProgress, progress, false).setSubText("$percentage%")
        return this
    }

    private data class MasterNotificationHolder(@get:Synchronized @set:Synchronized var progress: Int = 0) {
        var maxProgress: Int by Delegates.notNull()
    }

    private data class NotificationHolder(
            val id: Int,
            val notification: NotificationCompat.Builder,
            val maxProgress: Int,
            var progress: Int = 0)

    private companion object {
        /** Accounts for two load steps. */
        const val EXTRA_MASTER_OPS = 2
        /** Accounts for [EXTRA_MASTER_OPS] with two export files. */
        const val ESTIMATED_MASTER_OPS = 4

        /** Accounts one cleanup step. */
        const val EXTRA_EXPORT_OPS_SINGLE = 1
        /** Accounts for [EXTRA_EXPORT_OPS_SINGLE] and the average sheet step. */
        const val EXTRA_EXPORT_OPS_POLY = 2
    }
}
