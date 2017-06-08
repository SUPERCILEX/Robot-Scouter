package com.supercilex.robotscouter.data.client.spreadsheet;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.RequiresPermission;
import android.support.annotation.Size;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Pair;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.perf.metrics.AddTrace;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.client.NotificationForwarder;
import com.supercilex.robotscouter.data.model.Metric;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.util.Scouts;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.PermissionRequestHandler;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.charts.AxisCrosses;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.ChartAxis;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.ChartLegend;
import org.apache.poi.ss.usermodel.charts.DataSources;
import org.apache.poi.ss.usermodel.charts.LegendPosition;
import org.apache.poi.ss.usermodel.charts.LineChartData;
import org.apache.poi.ss.usermodel.charts.ScatterChartData;
import org.apache.poi.ss.usermodel.charts.ValueAxis;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pub.devrel.easypermissions.EasyPermissions;

import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.autoFitColumnWidths;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.createChartAnchor;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getAdjustedList;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getCellRangeAddress;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getChartRowIndex;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getMetricForChart;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getMetricForScouts;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getSafeSheetName;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.getStringForCell;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.isUnsupportedDevice;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.setChartAxisTitle;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.showError;
import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.showToast;
import static com.supercilex.robotscouter.data.model.MetricTypeKt.BOOLEAN;
import static com.supercilex.robotscouter.data.model.MetricTypeKt.HEADER;
import static com.supercilex.robotscouter.data.model.MetricTypeKt.LIST;
import static com.supercilex.robotscouter.data.model.MetricTypeKt.NUMBER;
import static com.supercilex.robotscouter.data.model.MetricTypeKt.STOPWATCH;
import static com.supercilex.robotscouter.data.model.MetricTypeKt.TEXT;
import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.logExportTeamsEvent;
import static com.supercilex.robotscouter.util.ConnectivityUtilsKt.isOffline;
import static com.supercilex.robotscouter.util.ConstantsKt.SINGLE_ITEM;
import static com.supercilex.robotscouter.util.ConstantsKt.providerAuthorityJava;
import static com.supercilex.robotscouter.util.IoUtilsKt.getRootFolder;
import static com.supercilex.robotscouter.util.IoUtilsKt.hideFile;
import static com.supercilex.robotscouter.util.IoUtilsKt.unhideFile;
import static com.supercilex.robotscouter.util.PreferencesUtilsKt.setShouldShowExportHint;
import static com.supercilex.robotscouter.util.PreferencesUtilsKt.shouldShowExportHint;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

public class SpreadsheetExporter extends IntentService implements OnSuccessListener<Map<TeamHelper, List<Scout>>> {
    private static final String TAG = "SpreadsheetExporter";

    private static final String MIME_TYPE_MS_EXCEL = "application/vnd.ms-excel";
    private static final String MIME_TYPE_ALL = "*/*";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final String UNSUPPORTED_FILE_EXTENSION = ".xls";

    static {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory",
                           "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory",
                           "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory",
                           "com.fasterxml.aalto.stax.EventFactoryImpl");
        WorkbookEvaluator.registerFunction("AVERAGEIF", SpreadsheetUtils.AVERAGEIF);
    }

    private Map<TeamHelper, List<Scout>> mScouts;
    private SpreadsheetCache mCache;

    public SpreadsheetExporter() {
        super(TAG);
        setIntentRedelivery(true);
    }

    /**
     * @return true if an export was attempted, false otherwise
     */
    public static boolean writeAndShareTeams(Fragment fragment,
                                             PermissionRequestHandler permHandler,
                                             @Size(min = 1) List<TeamHelper> teamHelpers) {
        if (teamHelpers.isEmpty()) return false;

        Context context = fragment.getContext();

        if (!EasyPermissions.hasPermissions(context, permHandler.getPermsArray())) {
            permHandler.requestPerms(R.string.write_storage_rationale_spreadsheet);
            return false;
        }

        if (shouldShowExportHint(context)) {
            Snackbar.make(fragment.getView(),
                          R.string.exporting_spreadsheet_hint,
                          Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.never_again, v -> setShouldShowExportHint(context, false))
                    .show();
        }

        fragment.getActivity()
                .startService(new Intent(context, SpreadsheetExporter.class)
                                      .putExtras(TeamHelper.toIntent(teamHelpers)));

        return true;
    }

    @Override
    @AddTrace(name = "onHandleIntent")
    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    protected void onHandleIntent(Intent intent) {
        mCache = new SpreadsheetCache(TeamHelper.parseList(intent), this);

        logExportTeamsEvent(mCache.getTeamHelpers());

        startForeground(R.string.exporting_spreadsheet_title,
                        mCache.getExportNotification(getString(R.string.exporting_spreadsheet_loading)));

        if (isOffline(this)) {
            showToast(this, getString(R.string.exporting_offline));
        }

        try {
            // Force a refresh
            Tasks.await(Scouts.getAll(mCache.getTeamHelpers(), this), 5, TimeUnit.MINUTES);

            mCache.updateNotification(getString(R.string.exporting_spreadsheet_loading));

            onSuccess(Tasks.await(
                    Scouts.getAll(mCache.getTeamHelpers(), this), 5, TimeUnit.MINUTES));
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            showError(this, e);
        }
    }

    @Override
    @AddTrace(name = "onSuccess")
    public void onSuccess(Map<TeamHelper, List<Scout>> scouts) {
        mScouts = Collections.unmodifiableMap(scouts);

        if (mScouts.size() != mCache.getTeamHelpers().size()) {
            // Some error occurred, let's try again
            startService(new Intent(this, SpreadsheetExporter.class)
                                 .putExtras(TeamHelper.toIntent(mCache.getTeamHelpers())));
            return;
        }

        mCache.onExportStarted();

        Uri spreadsheetUri = getFileUri();
        if (spreadsheetUri == null) return;

        int exportId = (int) System.currentTimeMillis();
        Intent baseIntent = new Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 26) { // NOPMD TODO Build.VERSION_CODES.O
            baseIntent.putStringArrayListExtra(
                    Intent.EXTRA_CONTENT_ANNOTATIONS,
                    new ArrayList<>(Collections.singletonList("document")));
        }

        Intent viewIntent = new Intent(baseIntent).setAction(Intent.ACTION_VIEW)
                .setDataAndType(spreadsheetUri, MIME_TYPE_MS_EXCEL)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (viewIntent.resolveActivity(getPackageManager()) == null) {
            viewIntent.setDataAndType(spreadsheetUri, MIME_TYPE_ALL);
        }

        Intent shareIntent = new Intent(baseIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent typeIntent = shareIntent.setAction(Intent.ACTION_SEND)
                    .setType(MIME_TYPE_MS_EXCEL)
                    .putExtra(Intent.EXTRA_STREAM, spreadsheetUri)
                    .putExtra(Intent.EXTRA_ALTERNATE_INTENTS, new Intent[]{viewIntent});

            shareIntent = Intent.createChooser(typeIntent,
                                               getPluralTeams(R.plurals.share_spreadsheet_title))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        } else {
            shareIntent = new Intent(viewIntent);
        }

        PendingIntent sharePendingIntent = PendingIntent.getActivity(
                this, exportId, shareIntent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_done_white_48dp)
                .setStyle(new NotificationCompat.BigTextStyle()
                                  .setBigContentTitle(getString(R.string.exporting_spreadsheet_complete_title))
                                  .setSummaryText(getPluralTeams(
                                          R.plurals.exporting_spreadsheet_complete_summary,
                                          mCache.getTeamHelpers().size()))
                                  .bigText(getPluralTeams(R.plurals.exporting_spreadsheet_complete_message)))
                .setContentIntent(sharePendingIntent)
                .addAction(R.drawable.ic_share_white_24dp,
                           getString(R.string.share),
                           PendingIntent.getBroadcast(
                                   this,
                                   exportId,
                                   NotificationForwarder.Companion.getCancelIntent(this,
                                                                                   exportId,
                                                                                   shareIntent),
                                   PendingIntent.FLAG_ONE_SHOT))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.addAction(R.drawable.ic_launch_white_24dp,
                              getString(R.string.open),
                              PendingIntent.getActivity(this,
                                                        exportId,
                                                        viewIntent,
                                                        PendingIntent.FLAG_UPDATE_CURRENT));
        }

        mCache.updateNotification(exportId, builder.build());
        stopForeground(true);
    }

    private String getPluralTeams(@PluralsRes int id) {
        return getPluralTeams(id, mCache.getTeamNames());
    }

    private String getPluralTeams(@PluralsRes int id, Object... args) {
        return getResources().getQuantityString(id, mCache.getTeamHelpers().size(), args);
    }

    @Nullable
    private Uri getFileUri() {
        @SuppressWarnings("MissingPermission")
        File rsFolder = getRootFolder();
        if (rsFolder == null) return null;

        File file = writeFile(rsFolder);
        if (file == null) return null;
        else return FileProvider.getUriForFile(this, providerAuthorityJava, file);
    }

    @Nullable
    private File writeFile(File rsFolder) {
        FileOutputStream stream = null;
        File absoluteFile = new File(rsFolder, getFullyQualifiedFileName(null));
        try {
            for (int i = 1; true; i++) {
                if (absoluteFile.exists()) {
                    absoluteFile = new File(
                            rsFolder, getFullyQualifiedFileName(" (" + i + ")"));
                } else {
                    absoluteFile = new File(absoluteFile.getParentFile(),
                                            hideFile(absoluteFile.getName()));
                    if (!absoluteFile.createNewFile()
                            // Attempt deleting existing hidden file (occurs when RS crashes while exporting)
                            && (!absoluteFile.delete() || !absoluteFile.createNewFile())) {
                        throw new IOException("Failed to create file");
                    }
                    break;
                }
            }

            stream = new FileOutputStream(absoluteFile);
            Workbook workbook;
            try {
                workbook = getWorkbook();
            } catch (Exception e) { // NOPMD
                absoluteFile.delete();
                throw e;
            }
            workbook.write(stream);

            return unhideFile(absoluteFile);
        } catch (IOException e) {
            showError(this, e);
            absoluteFile.delete();
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                FirebaseCrash.report(e);
            }
        }
        return null;
    }

    private String getFullyQualifiedFileName(@Nullable String middleMan) {
        String extension = isUnsupportedDevice() ? UNSUPPORTED_FILE_EXTENSION : FILE_EXTENSION;

        if (middleMan == null) return mCache.getTeamNames() + extension;
        else return mCache.getTeamNames() + middleMan + extension;
    }

    @AddTrace(name = "getWorkbook")
    private Workbook getWorkbook() {
        Workbook workbook;
        if (isUnsupportedDevice()) {
            workbook = new HSSFWorkbook();
            showToast(this, getString(R.string.unsupported_device));
        } else {
            workbook = new XSSFWorkbook();
        }
        mCache.setWorkbook(workbook);

        Sheet averageSheet = null;
        if (mCache.getTeamHelpers().size() > SINGLE_ITEM) {
            averageSheet = workbook.createSheet("Team Averages");
            averageSheet.createFreezePane(1, 1);
        }

        List<TeamHelper> teamHelpers = mCache.getTeamHelpers();
        for (TeamHelper teamHelper : teamHelpers) {
            mCache.updateNotification(getString(R.string.exporting_spreadsheet_team, teamHelper));

            Sheet teamSheet = workbook.createSheet(getSafeSheetName(workbook, teamHelper));
            teamSheet.createFreezePane(1, 1);
            buildTeamSheet(teamHelper, teamSheet);
        }

        mCache.updateNotification(getString(R.string.exporting_spreadsheet_average));
        if (averageSheet != null) buildTeamAveragesSheet(averageSheet);

        mCache.updateNotification(getString(R.string.exporting_spreadsheet_cleanup));
        autoFitColumnWidths(workbook);

        return workbook;
    }

    @AddTrace(name = "buildTeamSheet")
    private void buildTeamSheet(TeamHelper teamHelper, Sheet teamSheet) {
        List<Scout> scouts = mScouts.get(teamHelper);

        if (scouts.isEmpty()) {
            Workbook workbook = teamSheet.getWorkbook();
            workbook.removeSheetAt(workbook.getSheetIndex(teamSheet));
            return;
        }

        Row header = teamSheet.createRow(0);
        header.createCell(0); // Create empty top left corner cell
        List<Metric<?>> orderedMetrics = scouts.get(scouts.size() - 1).getMetrics();
        for (int i = 0; i < orderedMetrics.size(); i++) {
            Metric metric = orderedMetrics.get(i);
            Row row = teamSheet.createRow(i + 1);

            setupRow(row, teamHelper, metric);
        }

        for (int i = 0, column = 1; i < scouts.size(); i++, column++) {
            Scout scout = scouts.get(i);
            List<Metric<?>> metrics = scout.getMetrics();

            Cell cell = header.getCell(column, MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String name = scout.getName();
            cell.setCellValue(TextUtils.isEmpty(name) ? "Scout " + column : name);
            cell.setCellStyle(mCache.getColumnHeaderStyle());

            columnIterator:
            for (int j = 0, rowNum = 1; j < metrics.size(); j++, rowNum++) {
                Metric metric = metrics.get(j);

                Row row = teamSheet.getRow(rowNum);
                if (row == null) {
                    setupRowAndSetValue(teamSheet.createRow(rowNum), teamHelper, metric, column);
                } else {
                    List<Row> rows = getAdjustedList(teamSheet);

                    for (Row row1 : rows) {
                        Cell cell1 = row1.getCell(0);
                        if (TextUtils.equals(mCache.getMetricKey(row1), metric.getRef().getKey())) {
                            setRowValue(column, metric, row1);

                            if (TextUtils.isEmpty(cell1.getStringCellValue())) {
                                cell1.setCellValue(metric.getName());
                            }

                            continue columnIterator;
                        }
                    }

                    setupRowAndSetValue(teamSheet.createRow(teamSheet.getLastRowNum() + 1),
                                        teamHelper,
                                        metric,
                                        column);
                }
            }
        }


        if (scouts.size() > SINGLE_ITEM) {
            buildAverageColumn(teamSheet, teamHelper);
        }
    }

    private void setupRowAndSetValue(Row row, TeamHelper helper, Metric metric, int column) {
        setupRow(row, helper, metric);
        setRowValue(column, metric, row);
    }

    @AddTrace(name = "buildAverageColumn")
    private void buildAverageColumn(Sheet sheet, TeamHelper teamHelper) {
        int farthestColumn = 0;
        for (Row row : sheet) {
            int last = row.getLastCellNum();
            if (last > farthestColumn) farthestColumn = last;
        }

        Map<Chart, Pair<LineChartData, List<ChartAxis>>> chartData = new HashMap<>();
        Map<Metric<?>, Chart> chartPool = new HashMap<>();

        Iterator<Row> rowIterator = sheet.rowIterator();
        for (int i = 0; rowIterator.hasNext(); i++) {
            Row row = rowIterator.next();
            Cell cell = row.createCell(farthestColumn);
            if (i == 0) {
                cell.setCellValue(getString(R.string.average));
                cell.setCellStyle(mCache.getColumnHeaderStyle());
                continue;
            }

            Cell first = row.getCell(1, MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellStyle(first.getCellStyle());

            int type = getMetricForScouts(mScouts.get(teamHelper),
                                          mCache.getMetricKey(row)).getType();

            String rangeAddress = getCellRangeAddress(
                    first,
                    row.getCell(cell.getColumnIndex() - 1, MissingCellPolicy.CREATE_NULL_AS_BLANK));
            switch (type) {
                case BOOLEAN:
                    cell.setCellFormula("COUNTIF(" + rangeAddress + ", TRUE) / COUNTA(" + rangeAddress + ")");
                    mCache.setCellFormat(cell, "0.00%");
                    break;
                case NUMBER:
                    cell.setCellFormula(
                            "SUM(" + rangeAddress + ")" +
                                    " / " +
                                    "COUNT(" + rangeAddress + ")");

                    buildTeamChart(row, teamHelper, chartData, chartPool);
                    break;
                case STOPWATCH:
                    String excludeZeros = "\"<>0\"";
                    cell.setCellFormula(
                            "IF(COUNTIF(" + rangeAddress + ", " + excludeZeros +
                                    ") = 0, 0, AVERAGEIF(" + rangeAddress + ", " + excludeZeros + "))");

                    buildTeamChart(row, teamHelper, chartData, chartPool);
                    break;
                case LIST:
                    sheet.setArrayFormula(
                            "INDEX(" + rangeAddress + ", " +
                                    "MATCH(" +
                                    "MAX(" +
                                    "COUNTIF(" + rangeAddress + ", " + rangeAddress + ")" +
                                    "), " +
                                    "COUNTIF(" + rangeAddress + ", " + rangeAddress + ")" +
                                    ", 0))",
                            new CellRangeAddress(cell.getRowIndex(),
                                                 cell.getRowIndex(),
                                                 cell.getColumnIndex(),
                                                 cell.getColumnIndex()));
                    break;
                case HEADER:
                case TEXT:
                    // Nothing to average
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        for (Chart chart : chartData.keySet()) {
            Pair<LineChartData, List<ChartAxis>> data = chartData.get(chart);

            chart.plot(data.first, data.second.toArray(new ChartAxis[data.second.size()]));
            if (chart instanceof XSSFChart) {
                XSSFChart xChart = (XSSFChart) chart;
                CTChart ctChart = xChart.getCTChart();

                CTPlotArea plotArea = ctChart.getPlotArea();
                setChartAxisTitle(plotArea.getValAxArray(0).addNewTitle(), "Values");
                setChartAxisTitle(plotArea.getCatAxArray(0).addNewTitle(), "Scouts");

                String name = getMetricForChart(xChart, chartPool).getName();
                if (!TextUtils.isEmpty(name)) xChart.setTitle(name);
            }
        }
    }

    @AddTrace(name = "buildTeamChart")
    private void buildTeamChart(Row row,
                                TeamHelper teamHelper,
                                Map<Chart, Pair<LineChartData, List<ChartAxis>>> chartData,
                                Map<Metric<?>, Chart> chartPool) {
        if (isUnsupportedDevice()) return;

        Sheet sheet = row.getSheet();
        int rowNum = row.getRowNum();
        int lastDataCellNum = row.getSheet().getRow(0).getLastCellNum() - 2;

        Chart chart = null;
        Pair<Integer, Metric<?>> nearestHeader = null;

        List<Row> rows = getAdjustedList(row.getSheet());
        for (int i = row.getRowNum() - 1; i >= 0; i--) {
            Metric<?> metric = getMetricForScouts(mScouts.get(teamHelper),
                                                  mCache.getMetricKey(rows.get(i)));

            if (metric.getType() == HEADER) {
                nearestHeader = Pair.create(i, metric);

                Chart cachedChart = chartPool.get(metric);
                if (cachedChart != null) chart = cachedChart;
                break;
            }
        }

        chartFinder:
        if (nearestHeader == null) {
            for (Chart possibleChart : chartData.keySet()) {
                if (possibleChart instanceof XSSFChart) {
                    XSSFChart xChart = (XSSFChart) possibleChart;
                    if (xChart.getGraphicFrame().getAnchor().getRow1() == SINGLE_ITEM) {
                        nearestHeader = Pair.create(0, getMetricForChart(xChart, chartPool));
                        chart = xChart;
                        break chartFinder;
                    }
                }
            }

            nearestHeader = Pair.create(0, new Metric.Header("", null));
        }

        LineChartData data;
        if (chart == null) {
            Drawing drawing = sheet.createDrawingPatriarch();
            Integer headerIndex = nearestHeader.first + 1;
            int startChartIndex = lastDataCellNum + 3;
            chart = drawing.createChart(
                    createChartAnchor(drawing,
                                      getChartRowIndex(headerIndex,
                                                       new ArrayList<>(chartData.keySet())),
                                      startChartIndex,
                                      startChartIndex + 10));

            LineChartData lineChartData = chart.getChartDataFactory().createLineChartData();
            data = lineChartData;

            ChartAxis bottomAxis = chart.getChartAxisFactory()
                    .createCategoryAxis(AxisPosition.BOTTOM);
            ValueAxis leftAxis = chart.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            ChartLegend legend = chart.getOrCreateLegend();
            legend.setPosition(LegendPosition.RIGHT);

            chartData.put(chart, Pair.create(lineChartData, Arrays.asList(bottomAxis, leftAxis)));
            chartPool.put(nearestHeader.second, chart);
        } else {
            data = chartData.get(chart).first;
        }

        ChartDataSource<String> categorySource = DataSources.fromStringCellRange(
                sheet, new CellRangeAddress(0, 0, 1, lastDataCellNum));
        data.addSeries(
                categorySource,
                DataSources.fromNumericCellRange(
                        sheet,
                        new CellRangeAddress(rowNum, rowNum, 1, lastDataCellNum)))
                .setTitle(row.getCell(0).getStringCellValue());
    }

    private void setupRow(Row row, TeamHelper teamHelper, Metric metric) {
        Cell headerCell = row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK);

        mCache.putKeyMetric(headerCell, metric);

        if (metric.getType() == HEADER) {
            headerCell.setCellStyle(mCache.getHeaderMetricRowHeaderStyle());

            int numOfScouts = mScouts.get(teamHelper).size();
            if (numOfScouts > SINGLE_ITEM) {
                int rowNum = row.getRowNum();
                row.getSheet()
                        .addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, numOfScouts));
            }
        } else {
            headerCell.setCellStyle(mCache.getRowHeaderStyle());
        }
    }

    private void setRowValue(int column, Metric metric, Row row) {
        row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(metric.getName());

        Cell valueCell = row.getCell(column, MissingCellPolicy.CREATE_NULL_AS_BLANK);
        switch (metric.getType()) {
            case BOOLEAN:
                valueCell.setCellValue((boolean) metric.getValue());
                break;
            case NUMBER:
                Metric.Number numberMetric = (Metric.Number) metric;
                valueCell.setCellValue(numberMetric.getValue());

                String unit = numberMetric.getUnit();
                if (!TextUtils.isEmpty(unit)) {
                    mCache.setCellFormat(valueCell, "#0\"" + unit + "\"");
                }
                break;
            case LIST:
                Metric.List listMetric = (Metric.List) metric;
                String selectedItem =
                        listMetric.getValue().get(listMetric.getSelectedValueKey());
                valueCell.setCellValue(selectedItem);
                break;
            case TEXT:
                RichTextString note = mCache.getCreationHelper()
                        .createRichTextString(String.valueOf(metric.getValue()));
                valueCell.setCellValue(note);
                break;
            case STOPWATCH:
                List<? extends Long> cycles = ((Metric.Stopwatch) metric).getValue();

                long sum = 0;
                for (Long duration : cycles) sum += duration;
                long average = cycles.isEmpty() ? 0 : sum / cycles.size();

                valueCell.setCellValue(TimeUnit.MILLISECONDS.toSeconds(average));
                mCache.setCellFormat(valueCell, "#0\"s\"");
                break;
            case HEADER:
                // Headers are skipped because they don't contain any data
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @AddTrace(name = "buildTeamAveragesSheet")
    private void buildTeamAveragesSheet(Sheet averageSheet) {
        Workbook workbook = averageSheet.getWorkbook();
        Row headerRow = averageSheet.createRow(0);
        headerRow.createCell(0);

        List<Sheet> scoutSheets = getAdjustedList(workbook);
        for (int i = 0; i < scoutSheets.size(); i++) {
            Sheet scoutSheet = scoutSheets.get(i);
            Row row = averageSheet.createRow(i + 1);
            Cell rowHeaderCell = row.createCell(0);
            rowHeaderCell.setCellValue(scoutSheet.getSheetName());
            rowHeaderCell.setCellStyle(mCache.getRowHeaderStyle());

            List<Row> metricsRows = getAdjustedList(scoutSheet);
            rowIterator:
            for (Row averageRow : metricsRows) {
                Cell averageCell = averageRow.getCell(averageRow.getLastCellNum() - 1);
                Metric<?> keyMetric = mCache.getKeyMetric(averageRow.getCell(0));

                if (TextUtils.isEmpty(getStringForCell(averageCell))
                        || keyMetric.getType() == TEXT) {
                    continue;
                }

                for (Cell keyCell : getAdjustedList(headerRow)) {
                    if (TextUtils.equals(keyMetric.getRef().getKey(),
                                         mCache.getMetricKey(keyCell))) {
                        setAverageFormula(scoutSheet,
                                          row.createCell(keyCell.getColumnIndex()),
                                          averageCell);
                        continue rowIterator;
                    }
                }

                Cell keyCell = headerRow.createCell(headerRow.getLastCellNum());
                keyCell.setCellValue(averageRow.getCell(0).getStringCellValue());
                keyCell.setCellStyle(mCache.getColumnHeaderStyle());
                mCache.putKeyMetric(keyCell, keyMetric);

                setAverageFormula(scoutSheet,
                                  row.createCell(keyCell.getColumnIndex()),
                                  averageCell);
            }
        }

        buildAverageCharts(averageSheet);
    }

    @AddTrace(name = "buildAverageCharts")
    private void buildAverageCharts(Sheet sheet) {
        if (isUnsupportedDevice()) return;

        Drawing drawing = sheet.createDrawingPatriarch();

        List<Cell> headerCells = getAdjustedList(sheet.getRow(0));
        for (Cell cell : headerCells) {
            int columnIndex = cell.getColumnIndex();
            String headerName = cell.getStringCellValue();

            ClientAnchor anchor = createChartAnchor(
                    drawing, sheet.getLastRowNum() + 3, columnIndex, columnIndex + 1);
            anchor.setRow2(anchor.getRow2() + 30);
            Chart chart = drawing.createChart(anchor);
            chart.getOrCreateLegend().setPosition(LegendPosition.BOTTOM);

            ChartDataSource<String> categorySource = DataSources.fromArray(new String[]{headerName});
            ScatterChartData data = chart.getChartDataFactory().createScatterChartData();
            List<Row> dataRows = getAdjustedList(sheet);
            for (Row row : dataRows) {
                data.addSerie(
                        categorySource,
                        DataSources.fromNumericCellRange(
                                sheet,
                                new CellRangeAddress(row.getRowNum(),
                                                     row.getRowNum(),
                                                     columnIndex,
                                                     columnIndex)))
                        .setTitle(row.getCell(0).getStringCellValue());
            }

            ChartAxis bottomAxis = chart.getChartAxisFactory()
                    .createCategoryAxis(AxisPosition.BOTTOM);
            ValueAxis leftAxis = chart.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);
            chart.plot(data, bottomAxis, leftAxis);

            if (chart instanceof XSSFChart) {
                CTPlotArea plotArea = ((XSSFChart) chart).getCTChart().getPlotArea();

                setChartAxisTitle(plotArea.getValAxArray(0).addNewTitle(), "Values");
                setChartAxisTitle(plotArea.getCatAxArray(0).addNewTitle(), headerName);
            }
        }
    }

    private void setAverageFormula(Sheet scoutSheet, Cell valueCell, Cell averageCell) {
        String safeSheetName = scoutSheet.getSheetName().replace("'", "''");
        String rangeAddress = "'" + safeSheetName + "'!" + averageCell.getAddress();

        valueCell.setCellFormula("IF(" +
                                         "OR(" + rangeAddress + " = TRUE, " + rangeAddress + " = FALSE), " +
                                         "IF(" + rangeAddress + " = TRUE, 1, 0), " +
                                         rangeAddress + ")");
        valueCell.setCellStyle(averageCell.getCellStyle());

        if (averageCell.getCellTypeEnum() == CellType.BOOLEAN) {
            mCache.setCellFormat(valueCell, "0.00%");
        }
    }
}
