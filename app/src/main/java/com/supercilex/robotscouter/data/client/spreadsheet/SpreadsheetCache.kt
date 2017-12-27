package com.supercilex.robotscouter.data.client.spreadsheet

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.model.TeamCache
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook

class SpreadsheetCache(teams: Collection<Team>) : TeamCache(teams) {
    private val metricCache = mutableMapOf<Team, MutableMap<Int, Metric<*>>>()
    private val formatStyles = mutableMapOf<String, Short>()

    var workbook: Workbook by LateinitVal()
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

    val averageString: String by lazy { RobotScouter.INSTANCE.getString(R.string.metric_stopwatch_cycle_average_title) }

    fun getRootMetric(team: Team, index: Int): Metric<*>? = metricCache[team]!![index]

    fun putRootMetric(team: Team, index: Int, metric: Metric<*>) {
        (metricCache[team] ?: mutableMapOf<Int, Metric<*>>().also {
            metricCache.put(team, it)
        })[index] = metric
    }

    fun setCellFormat(cell: Cell, format: String) {
        if (isUnsupportedDevice) return

        cell.cellStyle = workbook.createCellStyle().apply {
            dataFormat = formatStyles[format]
                    ?: workbook.createDataFormat().getFormat(format).also {
                        formatStyles.put(format, it)
                    }
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
}
