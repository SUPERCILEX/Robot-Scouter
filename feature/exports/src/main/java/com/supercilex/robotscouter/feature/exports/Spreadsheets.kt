package com.supercilex.robotscouter.feature.exports

import android.graphics.Paint
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.isLowRamDevice
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.Team
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Chart
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.Drawing
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle

private const val MAX_SHEET_LENGTH = 31
private const val COLUMN_WIDTH_SCALE_FACTOR = 46
private const val CELL_WIDTH_CEILING = 7500

internal val isSupportedDevice by lazy { VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP && !isLowRamDevice }

internal val Cell?.stringValue: String
    get() {
        if (this == null) return ""
        return when (cellTypeEnum) {
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.NUMERIC -> numericCellValue.toString()
            CellType.STRING -> stringCellValue
            CellType.FORMULA -> cellFormula
            else -> ""
        }
    }

internal fun getSafeSheetName(workbook: Workbook, team: Team): String {
    var originalName = WorkbookUtil.createSafeSheetName(team.toString())
    var safeName = originalName
    var i = 1
    while (workbook.getSheet(safeName) != null) {
        safeName = "$originalName ($i)"
        if (safeName.length > MAX_SHEET_LENGTH) {
            originalName = team.number.toString()
            safeName = originalName
        }
        i++
    }

    return safeName
}

internal fun autoFitColumnWidths(sheetIterator: Iterable<Sheet>) {
    val paint = Paint()
    for (sheet in sheetIterator) {
        val numColumns = sheet.getRow(0).lastCellNum.toInt()
        for (i in 0 until numColumns) {
            var maxWidth = sheet
                    .map { it.getCell(i).stringValue }
                    .map { (paint.measureText(it) * COLUMN_WIDTH_SCALE_FACTOR).toInt() }
                    .max() ?: 2560

            // We don't want the columns to be too big
            maxWidth = if (maxWidth < CELL_WIDTH_CEILING) maxWidth else CELL_WIDTH_CEILING
            sheet.setColumnWidth(i, maxWidth)
        }
    }
}

internal fun getMetricForChart(chart: Chart, pool: Map<Metric<*>, Chart>): Metric<*> =
        pool.keys.first { pool[it] === chart }

internal fun CTTitle.setValue(text: String) {
    addNewLayout()
    addNewOverlay().`val` = false

    val textBody = addNewTx().addNewRich()
    textBody.addNewBodyPr()
    textBody.addNewLstStyle()

    val paragraph = textBody.addNewP()
    paragraph.addNewPPr().addNewDefRPr()
    paragraph.addNewR().t = text
    paragraph.addNewEndParaRPr()
}

internal fun Drawing<*>.createChartAnchor(
        startRow: Int,
        startColumn: Int,
        endColumn: Int
): ClientAnchor =
        createAnchor(0, 0, 0, 0, startColumn, startRow, endColumn, startRow + endColumn / 2)

internal fun showToast(message: String) = RobotScouter.runOnUiThread { longToast(message) }
