package com.supercilex.robotscouter.feature.exports

import android.content.Context
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.data.model.TeamCache
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.Team
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook

internal class SpreadsheetCache(
        private val context: Context,
        teams: Collection<Team>
) : TeamCache(teams) {
    private val metricCache = mutableMapOf<Team, MutableMap<Int, Metric<*>>>()
    private val lastDataOrAverageColumnIndices = mutableMapOf<Team, Int>()
    private val formatStyles = mutableMapOf<String, CellStyle>()

    var workbook: Workbook by LateinitVal()
    val creationHelper: CreationHelper by lazy { workbook.creationHelper }
    val formulaEvaluator: FormulaEvaluator by lazy { creationHelper.createFormulaEvaluator() }

    val columnHeaderStyle: CellStyle? by lazy {
        if (isSupportedDevice) createColumnHeaderStyle() else null
    }
    val calculatedColumnHeaderStyle: CellStyle? by lazy {
        if (isSupportedDevice) createCalculatedColumnHeaderStyle() else null
    }
    val rowHeaderStyle: CellStyle? by lazy {
        if (isSupportedDevice) createRowHeaderStyle() else null
    }
    val headerMetricRowHeaderStyle: CellStyle? by lazy {
        if (isSupportedDevice) createRowHeaderStyle().apply {
            setFont(createBaseHeaderFont().apply {
                italic = true
                fontHeightInPoints = 14.toShort()
            })
        } else null
    }

    val averageString: String by lazy { context.getString(R.string.export_average_column_title) }
    val medianString: String by lazy { context.getString(R.string.export_median_column_title) }
    val maxString: String by lazy { context.getString(R.string.export_max_column_title) }

    fun getRootMetric(team: Team, index: Int): Metric<*>? = metricCache.getValue(team)[index]

    fun getRootMetricIndices(metric: Metric<*>) = metricCache.mapNotNull { (team, metrics) ->
        metrics.toList().find { it.second.id == metric.id }?.first?.let { team to it }
    }

    fun putRootMetric(team: Team, index: Int, metric: Metric<*>) {
        metricCache.getOrPut(team) { mutableMapOf() }[index] = metric
    }

    fun getLastDataOrAverageColumnIndex(team: Team) = lastDataOrAverageColumnIndices.getValue(team)

    fun putLastDataOrAverageColumnIndex(team: Team, i: Int) {
        lastDataOrAverageColumnIndices[team] = i
    }

    fun setCellFormat(cell: Cell, format: String) {
        if (!isSupportedDevice) return

        cell.cellStyle = formatStyles.getOrPut(format) {
            workbook.createCellStyle().apply {
                dataFormat = workbook.createDataFormat().getFormat(format)
            }
        }
    }

    private fun createColumnHeaderStyle(): CellStyle = createBaseStyle().apply {
        setFont(createBaseHeaderFont())
        setAlignment(HorizontalAlignment.CENTER)
        setVerticalAlignment(VerticalAlignment.CENTER)
    }

    private fun createCalculatedColumnHeaderStyle(): CellStyle = createColumnHeaderStyle().apply {
        setFont(workbook.createFont().apply { italic = true })
    }

    private fun createRowHeaderStyle(): CellStyle =
            createColumnHeaderStyle().apply { setAlignment(HorizontalAlignment.LEFT) }

    private fun createBaseStyle(): CellStyle = workbook.createCellStyle().apply { wrapText = true }

    private fun createBaseHeaderFont(): Font = workbook.createFont().apply { bold = true }
}
