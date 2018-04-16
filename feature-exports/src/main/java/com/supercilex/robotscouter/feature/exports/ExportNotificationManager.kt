package com.supercilex.robotscouter.feature.exports

import android.support.v4.app.NotificationCompat
import android.support.v4.app.ServiceCompat
import android.support.v4.content.ContextCompat
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.EXPORT_IN_PROGRESS_CHANNEL
import com.supercilex.robotscouter.core.data.FilteringNotificationManager
import com.supercilex.robotscouter.core.data.isSingleton
import com.supercilex.robotscouter.core.data.model.getNames
import com.supercilex.robotscouter.core.data.notificationManager
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.Delegates

internal class ExportNotificationManager(private val service: ExportService) {
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
    private val exporters = ConcurrentHashMap<TemplateExporter, NotificationHolder>()

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
                .setContentText(RobotScouter.resources.getQuantityString(
                        R.plurals.export_load_status_detail,
                        teams.size,
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
    fun addExporter(exporter: TemplateExporter): Int {
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

    fun updateProgress(exporter: TemplateExporter, team: Team) =
            next(exporter, exportNotification.setContentText(team.toString()))

    fun onStartBuildingAverageSheet(exporter: TemplateExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.getString(R.string.export_average_status)))
    }

    fun onStartCleanup(exporter: TemplateExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.getString(R.string.export_cleanup_status)))
    }

    fun onStartJsonExport(exporter: TemplateExporter) {
        next(exporter, exportNotification
                .setContentText(RobotScouter.getString(R.string.export_json_status)))
    }

    @Synchronized
    fun removeExporter(exporter: TemplateExporter, notification: NotificationCompat.Builder) {
        val (id) = exporters.remove(exporter)!!
        // The original notification must be cancelled because we're changing channels
        notificationManager.cancel(id)
        notificationFilter.notify(
                id,
                notification.setGroup((hashCode() + 1).toString()).build(),
                true
        )

        if (pendingTaskCount == nTemplates) {
            notificationFilter.notify(hashCode() + 1, masterNotification
                    .setGroup((hashCode() + 1).toString())
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentText(RobotScouter.resources.getQuantityString(
                            R.plurals.export_complete_message, teams.size, teams.getNames()))
                    .setSubText(RobotScouter.resources.getQuantityString(
                            R.plurals.export_complete_subtitle, nTemplates, nTemplates))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build(), true)
        }

        if (--pendingTaskCount == 0) {
            stop()
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

    fun isStopped() = notificationFilter.isStopped()

    fun stop() {
        notificationFilter.cancel(hashCode())
        notificationFilter.stop()
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    fun abort() {
        notificationFilter.stopNow()
        for ((_, holder) in exporters) notificationManager.cancel(holder.id)
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun next(exporter: TemplateExporter, notification: NotificationCompat.Builder) {
        if (isStopped()) throw CancellationException()

        val holder = exporters.getValue(exporter)
        notificationFilter.notify(holder.id, notification
                .updateProgress(holder.maxProgress, ++holder.progress)
                .build())
    }

    private fun NotificationCompat.Builder.updateProgress(
            maxProgress: Int,
            progress: Int
    ): NotificationCompat.Builder {
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

        /** Accounts one cleanup step and the JSON export. */
        const val EXTRA_EXPORT_OPS_SINGLE = 2
        /** Accounts for the average sheet step. */
        const val EXTRA_EXPORT_OPS_POLY = EXTRA_EXPORT_OPS_SINGLE + 1
    }
}
