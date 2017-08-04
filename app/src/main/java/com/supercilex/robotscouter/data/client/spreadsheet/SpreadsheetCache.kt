package com.supercilex.robotscouter.data.client.spreadsheet

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.data.model.TeamCache
import com.supercilex.robotscouter.util.ui.EXPORT_IN_PROGRESS_CHANNEL
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import java.util.HashMap

class SpreadsheetCache(teams: Collection<Team>, context: Context) : TeamCache(teams) {
    private val progressMax =
            teams.size + if (teams.size == SINGLE_ITEM) EXTRA_OPS_SINGLE else EXTRA_OPS_POLY
    private var currentProgress = 0

    private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val progressNotification: NotificationCompat.Builder =
            NotificationCompat.Builder(context, EXPORT_IN_PROGRESS_CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(context.getString(R.string.export_in_progress_title))
                    .setProgress(progressMax, currentProgress, false)
                    .setSubText("0%")
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

    private val metricCache = HashMap<Team, HashMap<Int, Metric<*>>>()
    private val formatStyles = HashMap<String, Short>()

    lateinit var workbook: Workbook
    val creationHelper: CreationHelper by lazy { workbook.creationHelper }

    val columnHeaderStyle: CellStyle? by lazy {
        if (isUnsupportedDevice) null else createColumnHeaderStyle()
    }
    val rowHeaderStyle: CellStyle? by lazy {
        if (isUnsupportedDevice) null else createRowHeaderStyle()
    }
    val headerMetricRowHeaderStyle: CellStyle? by lazy {
        if (isUnsupportedDevice) null else createRowHeaderStyle().apply {
            setFont(createBaseHeaderFont().apply {
                italic = true
                fontHeightInPoints = 14.toShort()
            })
        }
    }

    fun getExportNotification(text: String): Notification =
            progressNotification.setContentText(text).build()

    fun updateNotification(text: String) {
        currentProgress++
        val percentage = Math.round(currentProgress.toFloat() / progressMax.toFloat() * 100)

        progressNotification.setProgress(progressMax, currentProgress, false)
        progressNotification.setSubText("$percentage%")
        updateNotification(R.string.export_in_progress_title, getExportNotification(text))
    }

    fun updateNotification(id: Int, notification: Notification) =
            notificationManager.notify(id, notification)

    fun onExportStarted() {
        progressNotification.setSmallIcon(android.R.drawable.stat_sys_upload)
        updateNotification(R.string.export_in_progress_title, progressNotification.build())
    }

    fun getRootMetric(team: Team, index: Int): Metric<*>? = metricCache[team]!![index]

    fun putRootMetric(team: Team, index: Int, metric: Metric<*>) {
        (metricCache[team] ?: HashMap<Int, Metric<*>>().also { metricCache.put(team, it) })[index] = metric
    }

    fun setCellFormat(cell: Cell, format: String) {
        if (isUnsupportedDevice) return

        cell.cellStyle = workbook.createCellStyle().apply {
            dataFormat = formatStyles[format] ?:
                    workbook.createDataFormat().getFormat(format).also { formatStyles.put(format, it) }
        }
    }

    private fun createColumnHeaderStyle(): CellStyle = createBaseStyle().apply {
        setFont(createBaseHeaderFont())
        setAlignment(HorizontalAlignment.CENTER)
        setVerticalAlignment(VerticalAlignment.CENTER)
    }

    private fun createRowHeaderStyle(): CellStyle =
            createColumnHeaderStyle().apply { setAlignment(HorizontalAlignment.LEFT) }

    private fun createBaseStyle(): CellStyle = workbook.createCellStyle().apply { wrapText = true }

    private fun createBaseHeaderFont(): Font = workbook.createFont().apply { bold = true }

    private companion object {
        /** Accounts for two load steps and one cleanup step. */
        const val EXTRA_OPS_SINGLE = 3
        /** Accounts for [EXTRA_OPS_SINGLE] and the average sheet step. */
        const val EXTRA_OPS_POLY = 4
    }
}
