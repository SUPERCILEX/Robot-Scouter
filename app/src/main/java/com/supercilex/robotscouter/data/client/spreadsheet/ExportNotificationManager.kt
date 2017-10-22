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
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.ui.EXPORT_IN_PROGRESS_CHANNEL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class ExportNotificationManager(private val service: ExportService) {
    private val manager =
            RobotScouter.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val masterNotification: NotificationCompat.Builder = NotificationCompat.Builder(
            RobotScouter.INSTANCE, EXPORT_IN_PROGRESS_CHANNEL)
            .setGroup(hashCode().toString())
            .setGroupSummary(true)
            .setContentTitle(RobotScouter.INSTANCE.getString(R.string.export_overall_progress_title))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setColor(ContextCompat.getColor(RobotScouter.INSTANCE, R.color.colorPrimary))
            .updateProgress(ESTIMATED_MASTER_OPS, 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    private val exportNotification: NotificationCompat.Builder = NotificationCompat.Builder(
            RobotScouter.INSTANCE, EXPORT_IN_PROGRESS_CHANNEL)
            .setGroup(hashCode().toString())
            .setContentTitle(RobotScouter.INSTANCE.getString(R.string.export_progress_title))
            .setContentText(RobotScouter.INSTANCE.getString(R.string.export_initialize_status))
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
                        .setContentText(RobotScouter.INSTANCE.getString(R.string.export_load_status))
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
                if (teams.isSingleton) EXTRA_EXPORT_OPS_SINGLE else EXTRA_EXPORT_OPS_POLY

        val notification = exportNotification.updateProgress(maxProgress, 0)
        exporters[exporter] = NotificationHolder(id, notification, maxProgress)

        manager.notify(id, notification.build())
        manager.notify(hashCode(), masterNotification
                .setContentText(RobotScouter.INSTANCE.getString(
                        R.string.export_template_status, exporter.templateName))
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
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_average_status))
                .build())
    }

    fun onStartCleanup(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_cleanup_status))
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
                            R.string.export_template_status,
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

        val percentage = (progress.toFloat() / maxProgress.toFloat() * 100).roundToInt()
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
