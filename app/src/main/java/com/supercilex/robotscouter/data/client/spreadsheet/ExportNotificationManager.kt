package com.supercilex.robotscouter.data.client.spreadsheet

import android.app.Notification
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
import com.supercilex.robotscouter.util.ui.SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS
import com.supercilex.robotscouter.util.ui.notificationManager
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class ExportNotificationManager(private val service: ExportService) {
    private val notificationPublisher = PublishSubject.create<Pair<Int, Notification>>()

    private val masterNotification: NotificationCompat.Builder get() = NotificationCompat.Builder(
            RobotScouter.INSTANCE, EXPORT_IN_PROGRESS_CHANNEL)
            .setGroup(hashCode().toString())
            .setGroupSummary(true)
            .setContentTitle(RobotScouter.INSTANCE.getString(R.string.export_overall_progress_title))
            .setColor(ContextCompat.getColor(RobotScouter.INSTANCE, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
    private val exportNotification: NotificationCompat.Builder get() = NotificationCompat.Builder(
            RobotScouter.INSTANCE, EXPORT_IN_PROGRESS_CHANNEL)
            .setGroup(hashCode().toString())
            .setContentTitle(RobotScouter.INSTANCE.getString(R.string.export_progress_title))
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setColor(ContextCompat.getColor(RobotScouter.INSTANCE, R.color.colorPrimary))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private val masterNotificationHolder = MasterNotificationHolder()
    private val exporters = ConcurrentHashMap<SpreadsheetExporter, NotificationHolder>()

    private var nTemplates: Int by LateinitVal()
    private var teams: Set<Team> by LateinitVal()
    private var pendingTaskCount: Int by Delegates.notNull()

    init {
        service.startForeground(hashCode(), masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_load_status))
                .updateProgress(ESTIMATED_MASTER_OPS, 0)
                .build())
    }

    fun setData(nTemplates: Int, teams: Set<Team>) {
        this.nTemplates = nTemplates
        this.teams = teams
        pendingTaskCount = nTemplates

        masterNotificationHolder.progress = EXTRA_MASTER_OPS
        masterNotificationHolder.maxProgress = EXTRA_MASTER_OPS + nTemplates

        notificationManager.notify(hashCode(), masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_load_status))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())

        notificationPublisher
                .groupBy { it.first }
                .flatMap {
                    it.sample(
                            // Multiply to ensure the overall rate limitation
                            SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS * nTemplates,
                            TimeUnit.MILLISECONDS,
                            true)
                }
                .subscribe {
                    notificationManager.notify(it.first, it.second)
                }
    }

    @Synchronized
    fun addExporter(exporter: SpreadsheetExporter): Int {
        check(exporters.size < pendingTaskCount) { "More exporters than templates" }

        val id = exporter.hashCode()
        val teams = exporter.scouts.keys
        val maxProgress = teams.size +
                if (teams.isSingleton) EXTRA_EXPORT_OPS_SINGLE else EXTRA_EXPORT_OPS_POLY

        notificationPublisher.onNext(id to exportNotification
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_initialize_status))
                .updateProgress(maxProgress, 0)
                .build())
        if (pendingTaskCount == nTemplates && exporters.isEmpty()) {
            notificationPublisher.onNext(hashCode() to masterNotification
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentText(RobotScouter.INSTANCE.getString(
                            R.string.export_template_status, exporter.templateName))
                    .updateProgress(
                            masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                    .build())
        }

        exporters.put(exporter, NotificationHolder(id, maxProgress))

        return id
    }

    fun updateProgress(exporter: SpreadsheetExporter, team: Team) =
            next(exporter, exportNotification.setContentText(team.toString()))

    fun onStartBuildingAverageSheet(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_average_status)))
    }

    fun onStartCleanup(exporter: SpreadsheetExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.INSTANCE.getString(R.string.export_cleanup_status)))
    }

    @Synchronized
    fun removeExporter(exporter: SpreadsheetExporter, notification: NotificationCompat.Builder) {
        val (id) = exporters.remove(exporter)!!
        // The original notification must be cancelled because we're changing channels
        notificationManager.cancel(id)
        notificationPublisher.onNext(id to notification.setGroup(hashCode().toString()).build())

        if (--pendingTaskCount == 0) {
            notificationPublisher.onNext(hashCode() to masterNotification
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentText(RobotScouter.INSTANCE.resources.getQuantityString(
                            R.plurals.export_complete_message, teams.size, teams.getNames()))
                    .setSubText(RobotScouter.INSTANCE.resources.getQuantityString(
                            R.plurals.export_complete_subtitle, nTemplates, nTemplates))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build())
            notificationPublisher.onComplete()
            ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_DETACH)
        } else {
            notificationPublisher.onNext(hashCode() to masterNotification
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
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
        for ((_, holder) in exporters) notificationManager.cancel(holder.id)
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun next(exporter: SpreadsheetExporter, notification: NotificationCompat.Builder) {
        val holder = exporters[exporter]!!
        notificationPublisher.onNext(holder.id to notification
                .updateProgress(holder.maxProgress, ++holder.progress)
                .build())
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

    private data class MasterNotificationHolder(var progress: Int = 0) {
        var maxProgress: Int by LateinitVal()
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
