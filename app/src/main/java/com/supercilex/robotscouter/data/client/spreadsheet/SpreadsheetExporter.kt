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
import com.google.firebase.firestore.FirebaseFirestore
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.MetricType
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.exportsFolder
import com.supercilex.robotscouter.util.data.hidden
import com.supercilex.robotscouter.util.data.unhide
import com.supercilex.robotscouter.util.isPolynomial
import com.supercilex.robotscouter.util.providerAuthority
import com.supercilex.robotscouter.util.ui.EXPORT_CHANNEL
import com.supercilex.robotscouter.util.ui.NotificationIntentForwarder
import kotlinx.coroutines.experimental.CancellationException
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Chart
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
import java.util.Locale
import java.util.concurrent.TimeUnit

class SpreadsheetExporter(
        scouts: Map<Team, List<Scout>>,
        private val notificationManager: ExportNotificationManager,
        private val rawTemplateName: String?
) {
    val templateName: String by lazy {
        rawTemplateName ?: RobotScouter.getString(R.string.export_unnamed_template_title)
    }
    val scouts: Map<Team, List<Scout>> = Collections.unmodifiableMap(scouts)
    private val cache = SpreadsheetCache(scouts.keys)

    fun export() {
        val exportId = notificationManager.addExporter(this)

        val spreadsheetUri = getFileUri()

        val baseIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            baseIntent.putStringArrayListExtra(
                    Intent.EXTRA_CONTENT_ANNOTATIONS,
                    ArrayList<String>().apply { add("document") }
            )
        }

        val viewIntent = Intent(baseIntent).setAction(Intent.ACTION_VIEW)
                .setDataAndType(spreadsheetUri, MIME_TYPE_MS_EXCEL)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (viewIntent.resolveActivity(RobotScouter.packageManager) == null) {
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
                RobotScouter, exportId, shareIntent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(RobotScouter, EXPORT_CHANNEL)
                .setSmallIcon(R.drawable.ic_done_white_24dp)
                .setContentTitle(RobotScouter.getString(
                        R.string.export_complete_title, templateName))
                .setSubText(getPluralTeams(R.plurals.export_complete_summary, cache.teams.size))
                .setContentText(getPluralTeams(R.plurals.export_complete_message))
                .setContentIntent(sharePendingIntent)
                .addAction(
                        R.drawable.ic_share_white_24dp,
                        RobotScouter.getString(R.string.share),
                        PendingIntent.getActivity(
                                RobotScouter,
                                exportId,
                                NotificationIntentForwarder.getCancelIntent(exportId, shareIntent),
                                PendingIntent.FLAG_ONE_SHOT)
                )
                .setColor(ContextCompat.getColor(RobotScouter, R.color.colorPrimary))
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.addAction(
                    R.drawable.ic_launch_white_24dp,
                    RobotScouter.getString(R.string.open),
                    PendingIntent.getActivity(
                            RobotScouter,
                            exportId,
                            viewIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT)
            )
        }

        if (!notificationManager.isStopped()) {
            notificationManager.removeExporter(this, builder)
        }
    }

    private fun getPluralTeams(@PluralsRes id: Int) = getPluralTeams(id, cache.teamNames)

    private fun getPluralTeams(@PluralsRes id: Int, vararg args: Any): String =
            RobotScouter.resources.getQuantityString(id, cache.teams.size, *args)

    private fun getFileUri(): Uri {
        val folder = synchronized(notificationManager) {
            checkNotNull(exportsFolder) { "Couldn't get write access" }
        }

        return FileProvider.getUriForFile(
                RobotScouter, providerAuthority, writeFile(folder))
    }

    private fun writeFile(rsFolder: File): File {
        val file = synchronized(notificationManager) {
            val availableFile = findAvailableFile(rsFolder).hidden()
            try {
                availableFile.apply {
                    if (
                        !parentFile.exists() && !parentFile.mkdirs() || !createNewFile()
                        // Attempt deleting existing hidden file (occurs when RS crashes while exporting)
                        && (!delete() || !createNewFile())
                    ) throw IOException("Failed to create file: $this")
                }
            } catch (e: IOException) {
                availableFile.delete()
                throw e
            }
        }

        var stream: FileOutputStream? = null
        try {
            stream = FileOutputStream(file)

            try {
                getWorkbook()
            } catch (e: Exception) {
                file.delete()
                throw e
            }.write(stream)

            return file.unhide() ?: throw IOException("Couldn't unhide file")
        } catch (e: IOException) {
            file.delete()
            throw e
        } finally {
            stream?.close()
        }
    }

    private fun findAvailableFile(rsFolder: File): File {
        var availableFile = File(rsFolder, getFullyQualifiedFileName(0))

        var i = 1
        while (true) {
            availableFile = if (availableFile.exists() || availableFile.hidden().exists()) {
                File(rsFolder, getFullyQualifiedFileName(i))
            } else {
                return availableFile
            }
            i++
        }
    }

    private fun getFullyQualifiedFileName(count: Int): String {
        val normalizedTemplateName =
                rawTemplateName?.toUpperCase(Locale.getDefault())?.replace(" ", "_")
        val prefix = if (normalizedTemplateName == null) "" else "[$normalizedTemplateName] "
        val suffix = if (count <= 0) "" else " ($count)"
        val extension = if (isUnsupportedDevice) UNSUPPORTED_FILE_EXTENSION else FILE_EXTENSION

        return "$prefix${cache.teamNames}$suffix$extension"
    }

    private fun getWorkbook(): Workbook {
        fun checkStatus() {
            if (notificationManager.isStopped()) throw CancellationException()
        }

        val workbook = if (isUnsupportedDevice) {
            showToast(RobotScouter.getString(R.string.export_unsupported_device_rationale))
            HSSFWorkbook()
        } else {
            XSSFWorkbook()
        }.also {
            cache.workbook = it
        }

        val averageSheet = run {
            if (cache.teams.isPolynomial) {
                workbook.createSheet("Team Averages").apply {
                    createFreezePane(1, 1)
                }
            } else null
        }

        for (team in cache.teams) {
            checkStatus()
            notificationManager.updateProgress(this, team)
            buildTeamSheet(team, workbook.createSheet(getSafeSheetName(workbook, team)).apply {
                createFreezePane(1, 1)
            })
        }

        if (averageSheet != null) {
            checkStatus()
            notificationManager.onStartBuildingAverageSheet(this)
            buildTeamAveragesSheet(averageSheet)
        }

        checkStatus()
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

        val metricCache = mutableMapOf<String, Int>()

        fun setRowValue(metric: Metric<*>, row: Row, column: Int) {
            val valueCell = row.getCell(column, CREATE_NULL_AS_BLANK)
            when (metric.type) {
                MetricType.BOOLEAN -> valueCell.setCellValue((metric as Metric.Boolean).value)
                MetricType.NUMBER -> {
                    val numberMetric = metric as Metric.Number
                    valueCell.setCellValue(numberMetric.value.toDouble())

                    val unit = numberMetric.unit
                    if (unit.isNullOrBlank()) {
                        cache.setCellFormat(valueCell, "0.00")
                    } else {
                        cache.setCellFormat(valueCell, "#0\"$unit\"")
                    }
                }
                MetricType.STOPWATCH -> {
                    val cycles = (metric as Metric.Stopwatch).value
                    if (cycles.isNotEmpty()) {
                        val builder = StringBuilder("AVERAGE(")
                                .append(TimeUnit.MILLISECONDS.toSeconds(cycles.first()))
                        for (cycle in cycles.subList(1, cycles.size)) {
                            builder.append(", ").append(TimeUnit.MILLISECONDS.toSeconds(cycle))
                        }
                        builder.append(')')

                        valueCell.cellFormula = builder.toString()
                    }

                    cache.setCellFormat(valueCell, "#0\"s\"")
                }
                MetricType.LIST -> {
                    val listMetric = metric as Metric.List
                    val selectedItem =
                            listMetric.value.firstOrNull { it.id == listMetric.selectedValueId }
                    valueCell.setCellValue(
                            selectedItem?.name ?: listMetric.value.firstOrNull()?.name)
                }
                MetricType.TEXT -> {
                    valueCell.setCellValue(
                            cache.creationHelper.createRichTextString((metric as Metric.Text).value))
                }
                MetricType.HEADER -> Unit // No data
            }
        }

        fun addTitleRowMergedRegion(row: Row) {
            if (scouts.isPolynomial) {
                row.rowNum.also {
                    teamSheet.addMergedRegion(CellRangeAddress(it, it, 1, scouts.size))
                }
            }
        }

        fun setupRow(metric: Metric<*>, index: Int): Row {
            metricCache[metric.ref.id] = index
            cache.putRootMetric(team, index, metric.let {
                return@let when (metric.type) {
                    MetricType.HEADER -> Metric.Header(it.name, position = index, ref = it.ref)
                    MetricType.BOOLEAN -> Metric.Boolean(it.name, position = index, ref = it.ref)
                    MetricType.NUMBER -> Metric.Number(it.name, position = index, ref = it.ref)
                    MetricType.STOPWATCH ->
                        Metric.Stopwatch(it.name, position = index, ref = it.ref)
                    MetricType.TEXT -> Metric.Text(it.name, position = index, ref = it.ref)
                    MetricType.LIST -> Metric.List(it.name, position = index, ref = it.ref)
                }
            })

            return teamSheet.createRow(index).apply {
                createCell(0).apply {
                    setCellValue(metric.name)
                    if (metric.type == MetricType.HEADER) {
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
                setCellValue(scout.name.let { if (it.isNullOrBlank()) "Scout ${i + 1}" else it })
                cellStyle = cache.columnHeaderStyle
            }

            for ((j, metric) in scout.metrics.withIndex()) {
                if (i == scouts.lastIndex) { // Initialize the metric list
                    setupRow(metric, j + 1).also { setRowValue(metric, it, i + 1) }
                } else {
                    val metricIndex = metricCache[metric.ref.id]
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

        cache.putLastDataOrAverageColumnIndex(team, teamSheet.getRow(0).lastCellNum
                + if (scouts.isPolynomial) 0 else -1)
        if (scouts.isPolynomial) buildTeamAverageColumn(teamSheet, team)
    }

    private fun buildTeamAverageColumn(sheet: Sheet, team: Team) {
        fun createHeader(column: Int, title: String) {
            sheet.getRow(0).getCell(column, CREATE_NULL_AS_BLANK).apply {
                setCellValue(title)
                cellStyle = cache.columnHeaderStyle
            }
        }

        fun Row.createCellWithStyle(column: Int): Cell = createCell(column).apply {
            cellStyle = getCell(1, CREATE_NULL_AS_BLANK).cellStyle
        }

        fun Cell.rangeAddress() = CellRangeAddress(rowIndex, rowIndex, columnIndex, columnIndex)

        infix fun Cell.to(other: Cell) = "$address:${other.address}"

        val averageColumn = sheet.getRow(0).lastCellNum.toInt()
        val medianColumn = averageColumn + 1

        createHeader(averageColumn, cache.averageString)
        createHeader(medianColumn, cache.medianString)

        val chartData = mutableMapOf<Chart, Pair<LineChartData, List<ChartAxis>>>()
        val chartPool = mutableMapOf<Metric<*>, Chart>()

        for (i in 1..sheet.lastRowNum) {
            val type = (cache.getRootMetric(team, i) ?: continue).type
            val row = sheet.getRow(i)

            val averageCell = row.createCellWithStyle(averageColumn)
            val medianCell = row.createCellWithStyle(medianColumn)
            val address = row.getCell(1, CREATE_NULL_AS_BLANK) to
                    row.getCell(averageColumn - 1, CREATE_NULL_AS_BLANK)

            when (type) {
                MetricType.BOOLEAN -> {
                    sheet.setArrayFormula("AVERAGE(IF($address, 1, 0))", averageCell.rangeAddress())
                    cache.setCellFormat(averageCell, "0.00%")

                    val averageAddress = averageCell.address
                    medianCell.cellFormula = "IF($averageAddress > 0.5, TRUE, " +
                            "IF($averageAddress < 0.5, FALSE, \"INDETERMINATE\"))"
                }
                MetricType.NUMBER -> {
                    averageCell.cellFormula = "AVERAGE($address)"
                    medianCell.cellFormula = "MEDIAN($address)"
                    buildTeamChart(row, team, chartData, chartPool)
                }
                MetricType.STOPWATCH -> {
                    val computeIfPresent: (String) -> String = {
                        "IF(COUNT($address) = 0, NA(), $it)"
                    }
                    averageCell.cellFormula = computeIfPresent("AVERAGE($address)")
                    medianCell.cellFormula = computeIfPresent("MEDIAN($address)")
                    buildTeamChart(row, team, chartData, chartPool)
                }
                MetricType.LIST -> {
                    sheet.setArrayFormula(
                            "INDEX($address, MATCH(MAX(COUNTIF($address, $address)), " +
                                    "COUNTIF($address, $address), 0))",
                            averageCell.rangeAddress()
                    )
                    medianCell.cellFormula = "NA()"
                }
                MetricType.HEADER, MetricType.TEXT -> {
                    averageCell.cellFormula = "NA()"
                    medianCell.cellFormula = "NA()"
                }
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
                if (name.isNotBlank()) chart.setTitleText(name)
            }
        }
    }

    private fun buildTeamChart(
            row: Row,
            team: Team,
            chartData: MutableMap<Chart, Pair<LineChartData, List<ChartAxis>>>,
            chartPool: MutableMap<Metric<*>, Chart>
    ) {
        fun getChartRowIndex(defaultIndex: Int, charts: Collection<Chart>): Int {
            if (charts.isEmpty()) return defaultIndex

            val anchors = charts.filterIsInstance<XSSFChart>().mapTo(mutableListOf()) {
                it.graphicFrame.anchor
            }
            if (anchors.isEmpty()) return defaultIndex

            anchors.sortWith(Comparator { o1, o2 ->
                val endRow1 = o1.row2
                val endRow2 = o2.row2

                if (endRow1 == endRow2) 0 else if (endRow1 > endRow2) 1 else -1
            })

            val lastRow = anchors[anchors.lastIndex].row2
            return if (defaultIndex > lastRow) defaultIndex else lastRow
        }

        if (isUnsupportedDevice) return

        var chart: Chart? = null
        val nearestHeader = fun(): Pair<Int, Metric<*>> {
            for (i in row.rowNum downTo 1) {
                val metric = cache.getRootMetric(team, i) ?: continue

                if (metric.type == MetricType.HEADER) {
                    val cachedChart = chartPool[metric]
                    if (cachedChart != null) chart = cachedChart
                    return i to metric
                }
            }

            for (possibleChart in chartData.keys) {
                if (possibleChart is XSSFChart
                        && possibleChart.graphicFrame.anchor.row1 == 1) {
                    chart = possibleChart
                    return 0 to getMetricForChart(possibleChart, chartPool)
                }
            }

            return 0 to Metric.Header(
                    position = 0,
                    ref = FirebaseFirestore.getInstance().document("null/null")
            )
        }.invoke()

        val lastDataCellNum = cache.getLastDataOrAverageColumnIndex(team)

        val data: LineChartData
        if (chart == null) {
            val drawing = row.sheet.createDrawingPatriarch()
            val startChartIndex = lastDataCellNum + 3
            val newChart = drawing.createChart(drawing.createChartAnchor(
                    getChartRowIndex(nearestHeader.first, chartData.keys),
                    startChartIndex,
                    startChartIndex + 10)
            )

            data = newChart.chartDataFactory.createLineChartData()

            val bottomAxis = newChart.chartAxisFactory.createCategoryAxis(AxisPosition.BOTTOM)
            val leftAxis = newChart.chartAxisFactory.createValueAxis(AxisPosition.LEFT)
            leftAxis.crosses = AxisCrosses.AUTO_ZERO

            val legend = newChart.orCreateLegend
            legend.position = LegendPosition.RIGHT

            chartData[newChart] = data to listOf(bottomAxis, leftAxis)
            chartPool[nearestHeader.second] = newChart
        } else {
            data = chartData[chart!!]!!.first
        }

        val categorySource = DataSources.fromStringCellRange(
                row.sheet,
                CellRangeAddress(0, 0, 1, lastDataCellNum)
        )
        data.addSeries(
                categorySource,
                DataSources.fromNumericCellRange(
                        row.sheet,
                        CellRangeAddress(row.rowNum, row.rowNum, 1, lastDataCellNum)
                )
        ).setTitle(row.getCell(0).stringCellValue)
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

        val metricCache = mutableMapOf<String, Int>()

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
                val metricIndex = metricCache[rootMetric.ref.id]
                val averageCell = scoutSheet.getRow(j)
                        .getCell(cache.getLastDataOrAverageColumnIndex(team))

                if (averageCell.stringValue.isBlank()
                        || rootMetric.type == MetricType.HEADER
                        || rootMetric.type == MetricType.TEXT) {
                    continue
                }

                if (metricIndex == null) {
                    val headerCell = headerRow.createCell(headerRow.lastCellNum.toInt()).apply {
                        setCellValue(rootMetric.name)
                        cellStyle = cache.columnHeaderStyle
                    }

                    metricCache[rootMetric.ref.id] = headerCell.columnIndex
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
                data.addSerie(
                        categorySource,
                        DataSources.fromNumericCellRange(
                                sheet,
                                CellRangeAddress(it.rowNum, it.rowNum, columnIndex, columnIndex)
                        )
                ).setTitle(it.getCell(0).stringCellValue)
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

        init {
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLInputFactory",
                    "com.fasterxml.aalto.stax.InputFactoryImpl"
            )
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                    "com.fasterxml.aalto.stax.OutputFactoryImpl"
            )
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLEventFactory",
                    "com.fasterxml.aalto.stax.EventFactoryImpl"
            )
        }
    }
}
