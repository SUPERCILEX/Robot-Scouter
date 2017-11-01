package com.supercilex.robotscouter.data.client.spreadsheet

import android.arch.core.executor.ArchTaskExecutor
import android.graphics.Paint
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.isLowRamDevice
import org.apache.poi.ss.formula.OperationEvaluationContext
import org.apache.poi.ss.formula.eval.ErrorEval
import org.apache.poi.ss.formula.eval.NumberEval
import org.apache.poi.ss.formula.eval.ValueEval
import org.apache.poi.ss.formula.functions.Countif
import org.apache.poi.ss.formula.functions.FreeRefFunction
import org.apache.poi.ss.formula.functions.Sumif
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Chart
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.Drawing
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.WorkbookUtil
import org.jetbrains.anko.longToast
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle

val averageifFunction: FreeRefFunction = object : FreeRefFunction {
    override fun evaluate(args: Array<ValueEval>, context: OperationEvaluationContext): ValueEval =
            if (args.size >= 2 && args.size % 2 == 0) {
                var result = 0.0

                var i = 0
                while (i < args.size) {
                    val firstArg = args[i]
                    val secondArg = args[i + 1]
                    val evaluate =
                            evaluate(context.rowIndex, context.columnIndex, firstArg, secondArg)

                    result = evaluate.numberValue
                    i += 2
                }

                NumberEval(result)
            } else {
                ErrorEval.VALUE_INVALID
            }

    private fun evaluate(srcRowIndex: Int,
                         srcColumnIndex: Int,
                         arg0: ValueEval,
                         arg1: ValueEval): NumberEval {
        val totalEval = Sumif().evaluate(srcRowIndex, srcColumnIndex, arg0, arg1) as NumberEval
        val countEval = Countif().evaluate(srcRowIndex, srcColumnIndex, arg0, arg1) as NumberEval

        return NumberEval(totalEval.numberValue / countEval.numberValue)
    }
}

private const val MAX_SHEET_LENGTH = 31
private const val COLUMN_WIDTH_SCALE_FACTOR = 46
private const val CELL_WIDTH_CEILING = 7500

val isUnsupportedDevice by lazy { VERSION.SDK_INT < VERSION_CODES.LOLLIPOP || isLowRamDevice }

val Cell?.stringValue: String get() {
    if (this == null) return ""
    return when (cellTypeEnum) {
        CellType.BOOLEAN -> booleanCellValue.toString()
        CellType.NUMERIC -> numericCellValue.toString()
        CellType.STRING -> stringCellValue
        CellType.FORMULA -> cellFormula
        else -> ""
    }
}

fun getCellRangeAddress(first: Cell, last: Cell) = "${first.address}:${last.address}"

fun getSafeSheetName(workbook: Workbook, team: Team): String {
    var originalName = WorkbookUtil.createSafeSheetName(team.toString())
    var safeName = originalName
    var i = 1
    while (true) {
        if (workbook.getSheet(safeName) == null) break
        else {
            safeName = "$originalName ($i)"
            if (safeName.length > MAX_SHEET_LENGTH) {
                originalName = team.number.toString()
                safeName = originalName
            }
        }
        i++
    }

    return safeName
}

fun autoFitColumnWidths(sheetIterator: Iterable<Sheet>) {
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

fun getMetricForChart(chart: Chart, pool: Map<Metric<*>, Chart>): Metric<*> {
    pool.keys.filter { pool[it] === chart }.forEach { return it }

    throw IllegalStateException("Chart not found in pool")
}

fun CTTitle.setValue(text: String) {
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

fun Drawing<*>.createChartAnchor(startRow: Int, startColumn: Int, endColumn: Int): ClientAnchor =
        createAnchor(0, 0, 0, 0, startColumn, startRow, endColumn, startRow + endColumn / 2)

fun showToast(message: String) = ArchTaskExecutor.getInstance().executeOnMainThread {
    RobotScouter.INSTANCE.longToast(message)
}
