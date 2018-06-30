package com.supercilex.robotscouter.feature.exports

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.ServiceCompat
import android.support.v4.content.ContextCompat
import androidx.core.net.toUri
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.data.EXPORT_IN_PROGRESS_CHANNEL
import com.supercilex.robotscouter.core.data.FilteringNotificationManager
import com.supercilex.robotscouter.core.data.MIME_TYPE_ANY
import com.supercilex.robotscouter.core.data.isSingleton
import com.supercilex.robotscouter.core.data.model.getNames
import com.supercilex.robotscouter.core.data.notificationManager
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.CancellationException
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import com.supercilex.robotscouter.R as RC

internal class ExportNotificationManager(private val service: ExportService) {
    private val notificationFilter = FilteringNotificationManager()

    private val transientGroupId = hashCode()
    private val permanentGroupId = transientGroupId + 1
    private val masterNotification: NotificationCompat.Builder
        get() = NotificationCompat.Builder(service, EXPORT_IN_PROGRESS_CHANNEL)
                .setGroup(transientGroupId.toString())
                .setGroupSummary(true)
                .setContentTitle(service.getString(R.string.export_overall_progress_title))
                .setColor(ContextCompat.getColor(service, RC.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
    private val exportNotification: NotificationCompat.Builder
        get() = NotificationCompat.Builder(service, EXPORT_IN_PROGRESS_CHANNEL)
                .setGroup(transientGroupId.toString())
                .setContentTitle(service.getString(R.string.export_progress_title))
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setColor(ContextCompat.getColor(service, RC.color.colorPrimary))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

    private val masterNotificationHolder = MasterNotificationHolder()
    private val exporters = ConcurrentHashMap<TemplateExporter, NotificationHolder>()

    private var nLoadingChunks: Int by LateinitVal()
    private var nTemplates: Int by LateinitVal()
    private var teams: Set<Team> by LateinitVal()
    private var exportFolder: File? = null
    private var pendingTaskCount: Int by Delegates.notNull()

    init {
        service.startForeground(transientGroupId, masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(service.getString(R.string.export_load_status))
                .updateProgress(ESTIMATED_MASTER_OPS, 0)
                .build())
        notificationFilter.start()
    }

    fun startLoading(chunks: Int) {
        nLoadingChunks = chunks
        masterNotificationHolder.maxProgress = EXTRA_MASTER_OPS + nLoadingChunks

        notificationFilter.notify(transientGroupId, masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(service.getString(R.string.export_load_status))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())
    }

    fun updateLoadProgress() {
        masterNotificationHolder.progress++
    }

    fun loading(teams: List<Team>) {
        notificationFilter.notify(transientGroupId, masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(service.resources.getQuantityString(
                        R.plurals.export_load_status_detail,
                        teams.size,
                        teams.first().number,
                        teams.last().number
                ))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())
    }

    fun setData(nTemplates: Int, teams: Set<Team>, exportFolder: File) {
        this.nTemplates = nTemplates
        this.teams = teams
        this.exportFolder = exportFolder
        pendingTaskCount = nTemplates

        masterNotificationHolder.progress = EXTRA_MASTER_OPS + nLoadingChunks
        masterNotificationHolder.maxProgress = EXTRA_MASTER_OPS + nLoadingChunks + nTemplates

        notificationFilter.notify(transientGroupId, masterNotification
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentText(service.getString(R.string.export_load_status))
                .updateProgress(
                        masterNotificationHolder.maxProgress, masterNotificationHolder.progress)
                .build())
    }

    @Synchronized
    fun addExporter(exporter: TemplateExporter): Int {
        if (isStopped()) throw CancellationException()
        check(exporters.size < pendingTaskCount) { "More exporters than templates" }

        val id = exporter.hashCode()
        val teams = exporter.scouts.keys
        val maxProgress = teams.size +
                if (teams.isSingleton) EXTRA_EXPORT_OPS_SINGLE else EXTRA_EXPORT_OPS_POLY

        notificationFilter.notify(id, exportNotification
                .setContentText(service.getString(R.string.export_initialize_status))
                .updateProgress(maxProgress, 0)
                .build())
        if (pendingTaskCount == nTemplates && exporters.isEmpty()) {
            notificationFilter.notify(transientGroupId, masterNotification
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentText(service.getString(
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
                .setContentText(service.getString(R.string.export_average_status)))
    }

    fun onStartCleanup(exporter: TemplateExporter) {
        next(exporter, exportNotification
                .setContentText(service.getString(R.string.export_cleanup_status)))
    }

    fun onStartJsonExport(exporter: TemplateExporter) {
        next(exporter, exportNotification
                .setContentText(service.getString(R.string.export_json_status)))
    }

    @Synchronized
    fun removeExporter(exporter: TemplateExporter, notification: NotificationCompat.Builder) {
        val (id) = checkNotNull(exporters.remove(exporter))

        // The original notification must be cancelled because we're changing channels
        notificationManager.cancel(id)
        notificationFilter.notify(
                id, notification.setGroup(permanentGroupId.toString()).build(), true)

        if (pendingTaskCount == nTemplates) showExportedPermanentNotification()

        if (--pendingTaskCount == 0) {
            stop()
        } else {
            notificationFilter.notify(transientGroupId, masterNotification
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentText(service.getString(
                            R.string.export_template_status,
                            exporters.keys.first().templateName
                    ))
                    .updateProgress(
                            masterNotificationHolder.maxProgress,
                            ++masterNotificationHolder.progress)
                    .build())
        }
    }

    fun isStopped() = notificationFilter.isStopped()

    fun stopEmpty() {
        nTemplates = 0
        teams = emptySet()

        showExportedPermanentNotification()
        stop()
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

    private fun showExportedPermanentNotification() {
        notificationFilter.notify(permanentGroupId, masterNotification
                .setGroup(permanentGroupId.toString())
                .setSmallIcon(RC.drawable.ic_logo)
                .setSubText(service.resources.getQuantityString(
                        R.plurals.export_complete_subtitle, nTemplates, nTemplates))
                .setContentText(if (teams.isEmpty()) {
                    service.getString(R.string.export_complete_none_message)
                } else {
                    service.resources.getQuantityString(
                            R.plurals.export_complete_message, teams.size, teams.getNames())
                })
                .apply {
                    val exportFolder = exportFolder?.path?.toUri() ?: return@apply
                    val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(exportFolder, MIME_TYPE_FOLDER)

                    val openIntent = if (intent.resolveActivity(service.packageManager) == null) {
                        intent.setDataAndType(exportFolder, MIME_TYPE_ANY)
                        Intent.createChooser(
                                intent, service.getString(R.string.export_browse_title))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    } else {
                        intent
                    }

                    setContentIntent(PendingIntent.getActivity(
                            service,
                            permanentGroupId,
                            openIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    ))
                }
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(), true)
    }

    private fun stop() {
        notificationFilter.cancel(transientGroupId)
        notificationFilter.stop()
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
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

        const val MIME_TYPE_FOLDER = "resource/folder"
    }
}
