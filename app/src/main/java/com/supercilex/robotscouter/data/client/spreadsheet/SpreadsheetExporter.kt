package com.supercilex.robotscouter.data.client.spreadsheet

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.annotation.PluralsRes
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.TextUtils
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.perf.metrics.AddTrace
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.client.NotificationForwarder
import com.supercilex.robotscouter.data.model.BOOLEAN
import com.supercilex.robotscouter.data.model.HEADER
import com.supercilex.robotscouter.data.model.LIST
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.NUMBER
import com.supercilex.robotscouter.data.model.STOPWATCH
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TEXT
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.SINGLE_ITEM
import com.supercilex.robotscouter.util.data.hideFile
import com.supercilex.robotscouter.util.data.rootFolder
import com.supercilex.robotscouter.util.data.unhideFile
import com.supercilex.robotscouter.util.providerAuthority
import com.supercilex.robotscouter.util.ui.EXPORT_CHANNEL
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Chart
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.charts.AxisCrosses
import org.apache.poi.ss.usermodel.charts.AxisPosition
import org.apache.poi.ss.usermodel.charts.ChartAxis
import org.apache.poi.ss.usermodel.charts.DataSources
import org.apache.poi.ss.usermodel.charts.LegendPosition
import org.apache.poi.ss.usermodel.charts.LineChartData
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFChart
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.concurrent.TimeUnit

class SpreadsheetExporter(scouts: Map<Team, List<Scout>>,
                          private val notificationManager: ExportNotificationManager,
                          val templateName: String) {
    val scouts: Map<Team, List<Scout>> = Collections.unmodifiableMap(scouts)
    private val cache = SpreadsheetCache(scouts.keys)

    fun export() {
        val exportId = notificationManager.addExporter(this)

        val spreadsheetUri = getFileUri() ?: return

        val baseIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            baseIntent.putStringArrayListExtra(
                    Intent.EXTRA_CONTENT_ANNOTATIONS,
                    ArrayList(listOf("document")))
        }

        val viewIntent = Intent(baseIntent).setAction(Intent.ACTION_VIEW)
                .setDataAndType(spreadsheetUri, MIME_TYPE_MS_EXCEL)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (viewIntent.resolveActivity(RobotScouter.INSTANCE.packageManager) == null) {
            viewIntent.setDataAndType(spreadsheetUri, MIME_TYPE_ALL)
        }

        val shareIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val typeIntent = Intent(baseIntent).setAction(Intent.ACTION_SEND)
                    .setType(MIME_TYPE_MS_EXCEL)
                    .putExtra(Intent.EXTRA_STREAM, spreadsheetUri)
                    .putExtra(Intent.EXTRA_ALTERNATE_INTENTS, arrayOf(viewIntent))

            Intent.createChooser(typeIntent, getPluralTeams(R.plurals.export_share_title))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        } else {
            Intent(viewIntent)
        }

        val sharePendingIntent = PendingIntent.getActivity(
                RobotScouter.INSTANCE, exportId, shareIntent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(RobotScouter.INSTANCE, EXPORT_CHANNEL)
                .setSmallIcon(R.drawable.ic_done_white_48dp)
                .setContentTitle(RobotScouter.INSTANCE.getString(
                        R.string.export_complete_title, templateName))
                .setSubText(getPluralTeams(R.plurals.export_complete_summary, cache.teams.size))
                .setContentText(getPluralTeams(R.plurals.export_complete_message))
                .setContentIntent(sharePendingIntent)
                .addAction(
                        R.drawable.ic_share_white_24dp,
                        RobotScouter.INSTANCE.getString(R.string.share),
                        PendingIntent.getBroadcast(
                                RobotScouter.INSTANCE,
                                exportId,
                                NotificationForwarder.getCancelIntent(exportId, shareIntent),
                                PendingIntent.FLAG_ONE_SHOT))
                .setColor(ContextCompat.getColor(RobotScouter.INSTANCE, R.color.colorPrimary))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.addAction(
                    R.drawable.ic_launch_white_24dp,
                    RobotScouter.INSTANCE.getString(R.string.open),
                    PendingIntent.getActivity(
                            RobotScouter.INSTANCE,
                            exportId,
                            viewIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT))
        }

        notificationManager.removeExporter(this, builder)
    }

    private fun getPluralTeams(@PluralsRes id: Int) = getPluralTeams(id, cache.teamNames)

    private fun getPluralTeams(@PluralsRes id: Int, vararg args: Any): String =
            RobotScouter.INSTANCE.resources.getQuantityString(id, cache.teams.size, *args)

    private fun getFileUri(): Uri? {
        return FileProvider.getUriForFile(
                RobotScouter.INSTANCE,
                providerAuthority,
                writeFile(rootFolder ?: return null) ?: return null)
    }

    private fun writeFile(rsFolder: File): File? {
        var stream: FileOutputStream? = null
        var absoluteFile = File(rsFolder, getFullyQualifiedFileName())
        try {
            var i = 1
            while (true) {
                if (absoluteFile.exists()) {
                    absoluteFile = File(rsFolder, getFullyQualifiedFileName(" ($i)"))
                } else {
                    absoluteFile = File(absoluteFile.parentFile, hideFile(absoluteFile.name))
                    if (!absoluteFile.createNewFile()
                            // Attempt deleting existing hidden file (occurs when RS crashes while exporting)
                            && (!absoluteFile.delete() || !absoluteFile.createNewFile())) {
                        throw IOException("Failed to create file")
                    }
                    break
                }
                i++
            }

            stream = FileOutputStream(absoluteFile)
            try {
                getWorkbook()
            } catch (e: Exception) {
                absoluteFile.delete()
                throw e
            }.write(stream)

            return unhideFile(absoluteFile)
        } catch (e: IOException) {
            abortCritical(e, notificationManager)
            absoluteFile.delete()
        } finally {
            if (stream != null)
                try {
                    stream.close()
                } catch (e: IOException) {
                    FirebaseCrash.report(e)
                }
        }
        return null
    }

    private fun getFullyQualifiedFileName(middleMan: String? = null): String {
        val extension = if (isUnsupportedDevice) UNSUPPORTED_FILE_EXTENSION else FILE_EXTENSION
        val templateName = templateName.toUpperCase().replace(" ", "_")

        return if (middleMan == null) "${templateName}_${cache.teamNames}$extension"
        else "${templateName}_${cache.teamNames}$middleMan$extension"
    }

    @AddTrace(name = "getWorkbook")
    private fun getWorkbook(): Workbook {
        val workbook: Workbook
        if (isUnsupportedDevice) {
            workbook = HSSFWorkbook()
            showToast(RobotScouter.INSTANCE.getString(R.string.unsupported_device))
        } else {
            workbook = XSSFWorkbook()
        }
        cache.workbook = workbook

        val averageSheet = (fun(): Sheet? = if (cache.teams.size > SINGLE_ITEM) {
            workbook.createSheet("Team Averages").apply { createFreezePane(1, 1) }
        } else null).invoke()

        for (team in cache.teams) {
            notificationManager.updateProgress(this, team)
            buildTeamSheet(team, workbook.createSheet(getSafeSheetName(workbook, team)).apply {
                createFreezePane(1, 1)
            })
        }

        if (averageSheet != null) {
            notificationManager.onStartBuildingAverageSheet(this)
            buildTeamAveragesSheet(averageSheet)
        }

        notificationManager.onStartCleanup(this)
        autoFitColumnWidths(workbook)

        return workbook
    }

    private fun buildTeamSheet(team: Team, teamSheet: Sheet) {
        val scouts = scouts[team]!!

        if (scouts.isEmpty()) {
            teamSheet.workbook.apply { removeSheetAt(getSheetIndex(teamSheet)) }
            return
        }

        val metricCache = HashMap<String, Int>()

        fun setRowValue(metric: Metric<*>, row: Row, column: Int) {
            row.getCell(0, CREATE_NULL_AS_BLANK).setCellValue(metric.name)

            val valueCell = row.getCell(column, CREATE_NULL_AS_BLANK)
            when (metric.type) {
                BOOLEAN -> valueCell.setCellValue((metric as Metric.Boolean).value)
                NUMBER -> {
                    val numberMetric = metric as Metric.Number
                    valueCell.setCellValue(numberMetric.value.toDouble())

                    val unit = numberMetric.unit
                    if (TextUtils.isEmpty(unit)) cache.setCellFormat(valueCell, "0.00")
                    else cache.setCellFormat(valueCell, "#0\"$unit\"")
                }
                STOPWATCH -> {
                    val cycles = (metric as Metric.Stopwatch).value ?: return
                    val average = if (cycles.isEmpty()) 0 else cycles.sum() / cycles.size

                    valueCell.setCellValue(TimeUnit.MILLISECONDS.toSeconds(average).toDouble())
                    cache.setCellFormat(valueCell, "#0\"s\"")
                }
                LIST -> {
                    val listMetric = metric as Metric.List
                    val selectedItem = listMetric.value[listMetric.selectedValueKey]
                    valueCell.setCellValue(selectedItem)
                }
                TEXT -> {
                    valueCell.setCellValue(
                            cache.creationHelper.createRichTextString((metric as Metric.Text).value))
                }
                HEADER -> { // No data
                }
                else -> throw IllegalStateException("Unknown metric type ${metric.type}")
            }
        }

        fun addTitleRowMergedRegion(row: Row) {
            val numOfScouts = scouts.size
            if (numOfScouts > SINGLE_ITEM) {
                row.rowNum.also {
                    teamSheet.addMergedRegion(CellRangeAddress(it, it, 1, numOfScouts))
                }
            }
        }

        fun setupRow(metric: Metric<*>, index: Int): Row {
            metricCache[metric.ref.key] = index
            cache.putRootMetric(team, index, metric.let {
                return@let when (metric.type) {
                    HEADER -> Metric.Header(it.name)
                    BOOLEAN -> Metric.Boolean(it.name)
                    NUMBER -> Metric.Number(it.name)
                    STOPWATCH -> Metric.Stopwatch(it.name)
                    TEXT -> Metric.Text(it.name)
                    LIST -> Metric.List(it.name)
                    else -> throw IllegalStateException("Unknown metric type ${metric.type}")
                }.apply { ref = it.ref }
            })

            return teamSheet.createRow(index).apply {
                createCell(0).apply {
                    if (metric.type == HEADER) {
                        cellStyle = cache.headerMetricRowHeaderStyle
                        addTitleRowMergedRegion(row)
                    } else {
                        cellStyle = cache.rowHeaderStyle
                    }
                }
            }
        }

        val header = teamSheet.createRow(0)
        header.createCell(0) // Create empty top left corner cell

        var hasOutdatedMetrics = false
        for (i in scouts.lastIndex downTo 0) {
            val scout = scouts[i]

            header.createCell(i + 1).apply {
                setCellValue(scout.name.let { if (TextUtils.isEmpty(it)) "Scout ${i + 1}" else it })
                cellStyle = cache.columnHeaderStyle
            }

            for ((j, metric) in scout.metrics.withIndex()) {
                if (i == scouts.lastIndex) { // Initialize the metric list
                    setupRow(metric, j + 1).also { setRowValue(metric, it, i + 1) }
                } else {
                    val metricIndex = metricCache[metric.ref.key]
                    if (metricIndex == null) {
                        if (!hasOutdatedMetrics) {
                            teamSheet.createRow(teamSheet.lastRowNum + 2).also {
                                it.createCell(0).apply {
                                    setCellValue("Outdated metrics")
                                    cellStyle = cache.headerMetricRowHeaderStyle
                                    addTitleRowMergedRegion(this.row)
                                }
                            }
                        }

                        setupRow(metric, teamSheet.lastRowNum + 1)
                                .also { setRowValue(metric, it, i + 1) }

                        hasOutdatedMetrics = true
                    } else {
                        setRowValue(metric, teamSheet.getRow(metricIndex), i + 1)
                    }
                }
            }
        }

        if (scouts.size > SINGLE_ITEM) buildTeamAverageColumn(teamSheet, team)
    }

    private fun buildTeamAverageColumn(sheet: Sheet, team: Team) {
        val farthestColumn = sheet.map { it.lastCellNum.toInt() }.max()!!

        sheet.getRow(0).getCell(farthestColumn, CREATE_NULL_AS_BLANK).apply {
            setCellValue(cache.averageString)
            cellStyle = cache.columnHeaderStyle
        }

        val chartData = HashMap<Chart, Pair<LineChartData, List<ChartAxis>>>()
        val chartPool = HashMap<Metric<*>, Chart>()

        for (i in 1..sheet.lastRowNum) {
            val type = (cache.getRootMetric(team, i) ?: continue).type
            val row = sheet.getRow(i)
            val first = row.getCell(1, CREATE_NULL_AS_BLANK)

            val cell = row.createCell(farthestColumn).apply { cellStyle = first.cellStyle }
            val rangeAddress = getCellRangeAddress(
                    first,
                    row.getCell(cell.columnIndex - 1, CREATE_NULL_AS_BLANK))

            when (type) {
                BOOLEAN -> {
                    cell.cellFormula = "COUNTIF($rangeAddress, TRUE) / COUNTA($rangeAddress)"
                    cache.setCellFormat(cell, "0.00%")
                }
                NUMBER -> {
                    cell.cellFormula = "SUM($rangeAddress) / COUNT($rangeAddress)"
                    buildTeamChart(row, team, chartData, chartPool)
                }
                STOPWATCH -> {
                    val excludeZeros = "\"<>0\""
                    cell.cellFormula = "IF(COUNTIF($rangeAddress, $excludeZeros) = 0, 0, " +
                            "AVERAGEIF($rangeAddress, $excludeZeros))"

                    buildTeamChart(row, team, chartData, chartPool)
                }
                LIST -> {
                    sheet.setArrayFormula(
                            "INDEX($rangeAddress, MATCH(MAX(COUNTIF($rangeAddress, $rangeAddress)), " +
                                    "COUNTIF($rangeAddress, $rangeAddress), 0))",
                            CellRangeAddress(
                                    cell.rowIndex,
                                    cell.rowIndex,
                                    cell.columnIndex,
                                    cell.columnIndex))
                }
                HEADER, TEXT -> { // Nothing to average
                }
                else -> throw IllegalStateException("Unknown metric type $type")
            }
        }

        for (chart in chartData.keys) {
            val data = chartData[chart]!!

            chart.plot(data.first, *data.second.toTypedArray())
            if (chart is XSSFChart) {
                val plotArea = chart.ctChart.plotArea
                plotArea.getValAxArray(0).addNewTitle().setValue("Values")
                plotArea.getCatAxArray(0).addNewTitle().setValue("Scouts")

                val name = getMetricForChart(chart, chartPool).name
                if (!TextUtils.isEmpty(name)) chart.setTitle(name)
            }
        }
    }

    private fun buildTeamChart(row: Row,
                               team: Team,
                               chartData: MutableMap<Chart, Pair<LineChartData, List<ChartAxis>>>,
                               chartPool: MutableMap<Metric<*>, Chart>) {
        fun getChartRowIndex(defaultIndex: Int, charts: List<Chart>): Int {
            if (charts.isEmpty()) return defaultIndex

            val anchors = ArrayList<ClientAnchor>()
            for (chart in charts) {
                if (chart is XSSFChart) anchors += chart.graphicFrame.anchor
                else return defaultIndex
            }

            Collections.sort(anchors) { o1, o2 ->
                val endRow1 = o1.row2
                val endRow2 = o2.row2

                if (endRow1 == endRow2) 0 else if (endRow1 > endRow2) 1 else -1
            }

            val lastRow = anchors[anchors.lastIndex].row2
            return if (defaultIndex > lastRow) defaultIndex else lastRow
        }

        if (isUnsupportedDevice) return

        val lastDataCellNum = row.sheet.getRow(0).lastCellNum - 2

        var chart: Chart? = null
        val nearestHeader = fun(): Pair<Int, Metric<*>> {
            for (i in row.rowNum downTo 1) {
                val metric = cache.getRootMetric(team, i) ?: continue

                if (metric.type == HEADER) {
                    val cachedChart = chartPool[metric]
                    if (cachedChart != null) chart = cachedChart
                    return i to metric
                }
            }

            for (possibleChart in chartData.keys) {
                if (possibleChart is XSSFChart
                        && possibleChart.graphicFrame.anchor.row1 == SINGLE_ITEM) {
                    chart = possibleChart
                    return 0 to getMetricForChart(possibleChart, chartPool)
                }
            }

            return 0 to Metric.Header()
        }.invoke()

        val data: LineChartData
        if (chart == null) {
            val drawing = row.sheet.createDrawingPatriarch()
            val startChartIndex = lastDataCellNum + 3
            @Suppress("NAME_SHADOWING")
            val chart = drawing.createChart(drawing.createChartAnchor(
                    getChartRowIndex(nearestHeader.first, ArrayList(chartData.keys)),
                    startChartIndex,
                    startChartIndex + 10))

            data = chart.chartDataFactory.createLineChartData()

            val bottomAxis = chart.chartAxisFactory.createCategoryAxis(AxisPosition.BOTTOM)
            val leftAxis = chart.chartAxisFactory.createValueAxis(AxisPosition.LEFT)
            leftAxis.crosses = AxisCrosses.AUTO_ZERO

            val legend = chart.orCreateLegend
            legend.position = LegendPosition.RIGHT

            chartData.put(chart, data to listOf(bottomAxis, leftAxis))
            chartPool.put(nearestHeader.second, chart)
        } else {
            data = chartData[chart!!]!!.first
        }

        val categorySource = DataSources.fromStringCellRange(
                row.sheet, CellRangeAddress(0, 0, 1, lastDataCellNum))
        data.addSeries(
                categorySource,
                DataSources.fromNumericCellRange(
                        row.sheet,
                        CellRangeAddress(row.rowNum, row.rowNum, 1, lastDataCellNum)))
                .setTitle(row.getCell(0).stringCellValue)
    }

    private fun buildTeamAveragesSheet(averageSheet: Sheet) {
        fun setAverageFormula(scoutSheet: Sheet, valueCell: Cell, averageCell: Cell) {
            val safeSheetName = scoutSheet.sheetName.replace("'", "''")
            val rangeAddress = "'$safeSheetName'!${averageCell.address}"

            valueCell.cellFormula = "IF(OR($rangeAddress = TRUE, $rangeAddress = FALSE), " +
                    "IF($rangeAddress = TRUE, 1, 0), $rangeAddress)"
            valueCell.cellStyle = averageCell.cellStyle

            if (averageCell.cellTypeEnum == CellType.BOOLEAN) {
                cache.setCellFormat(valueCell, "0.00%")
            }
        }

        val workbook = averageSheet.workbook
        val headerRow = averageSheet.createRow(0)
        headerRow.createCell(0)

        val metricCache = HashMap<String, Int>()

        val sheetList = workbook.toList()
        for (i in 1 until workbook.numberOfSheets) { // Excluding the average sheet
            val scoutSheet = sheetList[i]
            val row = averageSheet.createRow(i)
            row.createCell(0).apply {
                setCellValue(scoutSheet.sheetName)
                cellStyle = cache.rowHeaderStyle
            }

            val team = cache.teams[workbook.getSheetIndex(scoutSheet) - 1]
            for (j in 1..scoutSheet.lastRowNum) {
                val rootMetric = cache.getRootMetric(team, j) ?: continue
                val metricIndex = metricCache[rootMetric.ref.key]
                val averageCell = scoutSheet.getRow(j).run { getCell(lastCellNum - 1) }

                if (TextUtils.isEmpty(averageCell.stringValue) || rootMetric.type == TEXT) continue

                if (metricIndex == null) {
                    val headerCell = headerRow.createCell(headerRow.lastCellNum.toInt()).apply {
                        setCellValue(rootMetric.name)
                        cellStyle = cache.columnHeaderStyle
                    }

                    metricCache[rootMetric.ref.key] = headerCell.columnIndex
                    setAverageFormula(
                            scoutSheet, row.createCell(headerCell.columnIndex), averageCell)
                } else {
                    setAverageFormula(scoutSheet, row.createCell(metricIndex), averageCell)
                }
            }
        }

        buildTeamAveragesCharts(averageSheet)
    }

    private fun buildTeamAveragesCharts(sheet: Sheet) {
        if (isUnsupportedDevice) return

        val drawing = sheet.createDrawingPatriarch()

        for (i in 1 until sheet.getRow(0).lastCellNum) {
            val cell = sheet.getRow(0).getCell(i)
            val columnIndex = cell.columnIndex
            val headerName = cell.stringCellValue

            val anchor =
                    drawing.createChartAnchor(sheet.lastRowNum + 3, columnIndex, columnIndex + 1)
            anchor.row2 = anchor.row2 + 30
            val chart = drawing.createChart(anchor)
            chart.orCreateLegend.position = LegendPosition.BOTTOM

            val categorySource = DataSources.fromArray(arrayOf<String>(headerName))
            val data = chart.chartDataFactory.createScatterChartData()
            (1..sheet.lastRowNum).map { sheet.getRow(it) }.forEach {
                data.addSerie(categorySource,
                              DataSources.fromNumericCellRange(
                                      sheet,
                                      CellRangeAddress(
                                              it.rowNum, it.rowNum, columnIndex, columnIndex)))
                        .setTitle(it.getCell(0).stringCellValue)
            }

            val bottomAxis = chart.chartAxisFactory.createCategoryAxis(AxisPosition.BOTTOM)
            val leftAxis = chart.chartAxisFactory.createValueAxis(AxisPosition.LEFT)
            leftAxis.crosses = AxisCrosses.AUTO_ZERO
            chart.plot(data, bottomAxis, leftAxis)

            if (chart is XSSFChart) {
                val plotArea = chart.ctChart.plotArea
                plotArea.getValAxArray(0).addNewTitle().setValue("Values")
                plotArea.getCatAxArray(0).addNewTitle().setValue(headerName)
            }
        }
    }

    private companion object {
        const val MIME_TYPE_MS_EXCEL = "application/vnd.ms-excel"
        const val MIME_TYPE_ALL = "*/*"
        const val FILE_EXTENSION = ".xlsx"
        const val UNSUPPORTED_FILE_EXTENSION = ".xls"
    }
}
