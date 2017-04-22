package com.supercilex.robotscouter.data.client;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.RequiresPermission;
import android.support.annotation.Size;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;
import com.supercilex.robotscouter.data.model.metrics.StopwatchMetric;
import com.supercilex.robotscouter.data.util.Scouts;
import com.supercilex.robotscouter.data.util.TeamCache;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.AnalyticsHelper;
import com.supercilex.robotscouter.util.ConnectivityHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.IoHelper;
import com.supercilex.robotscouter.util.PermissionRequestHandler;
import com.supercilex.robotscouter.util.PreferencesHelper;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Countif;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.functions.Sumif;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
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
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

public class SpreadsheetExporter extends IntentService implements OnSuccessListener<Map<TeamHelper, List<Scout>>> {
    private static final String TAG = "SpreadsheetExporter";

    private static final String DEVICE_MODEL;
    private static final List<String> UNSUPPORTED_DEVICES =
            Collections.unmodifiableList(Arrays.asList("SAMSUNG-SM-N900A"));

    private static final String MIME_TYPE_MS_EXCEL = "application/vnd.ms-excel";
    private static final String MIME_TYPE_ALL = "*/*";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final String UNSUPPORTED_FILE_EXTENSION = ".xls";
    private static final int MAX_SHEET_LENGTH = 31;
    private static final int COLUMN_WIDTH_SCALE_FACTOR = 46;
    private static final int CELL_WIDTH_CEILING = 7500;

    private static final FreeRefFunction AVERAGEIF = new FreeRefFunction() {
        @Override
        public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext context) {
            if (args.length >= 2 && args.length % 2 == 0) {
                double result = 0;

                for (int i = 0; i < args.length; i += 2) {
                    ValueEval firstArg = args[i];
                    ValueEval secondArg = args[i + 1];
                    NumberEval evaluate = evaluate(context.getRowIndex(),
                                                   context.getColumnIndex(),
                                                   firstArg,
                                                   secondArg);

                    result = evaluate.getNumberValue();
                }

                return new NumberEval(result);
            } else {
                return ErrorEval.VALUE_INVALID;
            }
        }

        private NumberEval evaluate(int srcRowIndex,
                                    int srcColumnIndex,
                                    ValueEval arg0,
                                    ValueEval arg1) {
            NumberEval totalEval =
                    (NumberEval) new Sumif().evaluate(srcRowIndex, srcColumnIndex, arg0, arg1);
            NumberEval countEval =
                    (NumberEval) new Countif().evaluate(srcRowIndex, srcColumnIndex, arg0, arg1);

            return new NumberEval(totalEval.getNumberValue() / countEval.getNumberValue());
        }
    };

    static {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory",
                           "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory",
                           "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory",
                           "com.fasterxml.aalto.stax.EventFactoryImpl");
        WorkbookEvaluator.registerFunction("AVERAGEIF", AVERAGEIF);

        DEVICE_MODEL = (Build.MANUFACTURER.toUpperCase(Locale.ROOT) + "-" +
                Build.MODEL.toUpperCase(Locale.ROOT)).replace(' ', '-');
    }

    private Map<TeamHelper, List<Scout>> mScouts;
    private Cache mCache;

    public SpreadsheetExporter() {
        super(TAG);
        setIntentRedelivery(true);
    }

    /**
     * @return true if an export was attempted, false otherwise
     */
    @SuppressWarnings("MissingPermission")
    public static boolean writeAndShareTeams(Fragment fragment,
                                             PermissionRequestHandler permHandler,
                                             @Size(min = 1) List<TeamHelper> teamHelpers) {
        if (teamHelpers.isEmpty()) return false;

        Context context = fragment.getContext();

        if (!EasyPermissions.hasPermissions(context, permHandler.getPermsArray())) {
            permHandler.requestPerms(R.string.write_storage_rationale_spreadsheet);
            return false;
        }

        if (PreferencesHelper.shouldShowExportHint(context)) {
            Snackbar.make(fragment.getView(),
                          R.string.exporting_spreadsheet_hint,
                          Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.never_again,
                               v -> PreferencesHelper.setShouldShowExportHint(context, false))
                    .show();
        }

        fragment.getActivity()
                .startService(new Intent(context, SpreadsheetExporter.class)
                                      .putExtras(TeamHelper.toIntent(teamHelpers)));

        return true;
    }

    private static boolean isUnsupportedDevice() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || UNSUPPORTED_DEVICES.contains(DEVICE_MODEL);
    }

    private void updateNotification(Notification notification) {
        updateNotification(R.string.exporting_spreadsheet_title, notification);
    }

    private void updateNotification(int id, Notification notification) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, notification);
    }

    private Notification getExportNotification(String text) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(getString(R.string.exporting_spreadsheet_title))
                .setContentText(text)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        return builder.build();
    }

    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @Override
    protected void onHandleIntent(Intent intent) {
        startForeground(R.string.exporting_spreadsheet_title,
                        getExportNotification(getString(R.string.exporting_spreadsheet_loading)));

        if (ConnectivityHelper.isOffline(this)) {
            showToast(getString(R.string.exporting_offline));
        }

        List<TeamHelper> teamHelpers = TeamHelper.parseList(intent);
        Collections.sort(teamHelpers);
        try {
            Tasks.await(Scouts.getAll(teamHelpers, this)); // Force a refresh

            Task<Map<TeamHelper, List<Scout>>> fetchTeamsTask =
                    Scouts.getAll(teamHelpers, this).addOnFailureListener(this::showError);
            Tasks.await(fetchTeamsTask);
            if (fetchTeamsTask.isSuccessful()) onSuccess(fetchTeamsTask.getResult());
        } catch (ExecutionException | InterruptedException e) {
            showError(e);
        }
    }

    @Override
    public void onSuccess(Map<TeamHelper, List<Scout>> scouts) {
        mScouts = scouts;
        mCache = new Cache(mScouts.keySet());

        AnalyticsHelper.exportTeams(mCache.getTeamHelpers());

        Uri spreadsheetUri = getFileUri();
        if (spreadsheetUri == null) return;

        int exportId = (int) System.currentTimeMillis();
        Intent sharingIntent = new Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent typeIntent = sharingIntent.setAction(Intent.ACTION_SEND)
                    .setType(MIME_TYPE_MS_EXCEL)
                    .putExtra(Intent.EXTRA_STREAM, spreadsheetUri);

            sharingIntent = Intent.createChooser(typeIntent,
                                                 getPluralTeams(R.plurals.share_spreadsheet_title))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        } else {
            sharingIntent.setAction(Intent.ACTION_VIEW)
                    .setDataAndType(spreadsheetUri, MIME_TYPE_MS_EXCEL);

            if (sharingIntent.resolveActivity(getPackageManager()) == null) {
                sharingIntent.setDataAndType(spreadsheetUri, MIME_TYPE_ALL);
            }
        }

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_done_white_48dp)
                .setContentTitle(getPluralTeams(R.plurals.exporting_spreadsheet_complete_title))
                .setContentIntent(PendingIntent.getActivity(this,
                                                            exportId,
                                                            sharingIntent,
                                                            PendingIntent.FLAG_ONE_SHOT))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            builder.setShowWhen(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        updateNotification(exportId, builder.build());
        stopForeground(true);
    }

    private String getPluralTeams(@PluralsRes int id) {
        return getResources().getQuantityString(id, mScouts.size(), mCache.getTeamNames());
    }

    @Nullable
    private Uri getFileUri() {
        @SuppressWarnings("MissingPermission")
        File rsFolder = IoHelper.getRootFolder();
        if (rsFolder == null) return null;

        File file = writeFile(rsFolder);
        return file == null ? null : Uri.fromFile(file);
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
                                            IoHelper.hide(absoluteFile.getName()));
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

            return IoHelper.unhide(absoluteFile);
        } catch (IOException e) {
            showError(e);
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

    private Workbook getWorkbook() {
        Workbook workbook;
        if (isUnsupportedDevice()) {
            workbook = new HSSFWorkbook();
            showToast(getString(R.string.unsupported_device));
        } else {
            workbook = new XSSFWorkbook();
        }
        mCache.setWorkbook(workbook);

        Sheet averageSheet = null;
        if (mScouts.size() > Constants.SINGLE_ITEM) {
            averageSheet = workbook.createSheet("Team Averages");
            averageSheet.createFreezePane(1, 1);
        }

        List<TeamHelper> teamHelpers = mCache.getTeamHelpers();
        for (TeamHelper teamHelper : teamHelpers) {
            updateNotification(getExportNotification(getString(R.string.exporting_spreadsheet_team,
                                                               teamHelper)));

            Sheet teamSheet = workbook.createSheet(getSafeName(workbook, teamHelper));
            teamSheet.createFreezePane(1, 1);
            buildTeamSheet(teamHelper, teamSheet);
        }

        updateNotification(getExportNotification(getString(R.string.exporting_spreadsheet_average)));
        if (averageSheet != null) buildTeamAveragesSheet(averageSheet);

        setColumnWidths(workbook);
        mCache.clearTemporaryCommentCells();

        return workbook;
    }

    private String getSafeName(Workbook workbook, TeamHelper teamHelper) {
        String originalName = WorkbookUtil.createSafeSheetName(teamHelper.toString());
        String safeName = originalName;
        for (int i = 1; true; i++) {
            if (workbook.getSheet(safeName) == null) {
                break;
            } else {
                safeName = originalName + " (" + i + ")";
                if (safeName.length() > MAX_SHEET_LENGTH) {
                    originalName = teamHelper.getTeam().getNumber();
                    safeName = originalName;
                }
            }
        }

        return safeName;
    }

    private void buildTeamSheet(TeamHelper teamHelper, Sheet teamSheet) {
        List<Scout> scouts = mScouts.get(teamHelper);

        Row header = teamSheet.createRow(0);
        header.createCell(0); // Create empty top left corner cell
        List<ScoutMetric> orderedMetrics = scouts.get(scouts.size() - 1).getMetrics();
        for (int i = 0; i < orderedMetrics.size(); i++) {
            ScoutMetric metric = orderedMetrics.get(i);
            Row row = teamSheet.createRow(i + 1);

            setupRow(row, teamHelper, metric);
        }

        for (int i = 0, column = 1; i < scouts.size(); i++, column++) {
            Scout scout = scouts.get(i);
            List<ScoutMetric> metrics = scout.getMetrics();

            Cell cell = header.getCell(column, MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String name = scout.getName();
            cell.setCellValue(TextUtils.isEmpty(name) ? "Scout " + column : name);
            cell.setCellStyle(mCache.getColumnHeaderStyle());

            columnIterator:
            for (int j = 0, rowNum = 1; j < metrics.size(); j++, rowNum++) {
                ScoutMetric metric = metrics.get(j);

                Row row = teamSheet.getRow(rowNum);
                if (row == null) {
                    setupRowAndSetValue(teamSheet.createRow(rowNum), teamHelper, metric, column);
                } else {
                    List<Row> rows = getAdjustedList(teamSheet);

                    for (Row row1 : rows) {
                        Cell cell1 = row1.getCell(0);
                        if (TextUtils.equals(getMetricKey(row1), metric.getKey())) {
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


        if (scouts.size() > Constants.SINGLE_ITEM) {
            buildAverageCells(teamSheet, teamHelper);
        }
    }

    private void setupRowAndSetValue(Row row, TeamHelper helper, ScoutMetric metric, int column) {
        setupRow(row, helper, metric);
        setRowValue(column, metric, row);
    }

    private void buildAverageCells(Sheet sheet, TeamHelper teamHelper) {
        int farthestColumn = 0;
        for (Row row : sheet) {
            int last = row.getLastCellNum();
            if (last > farthestColumn) farthestColumn = last;
        }

        Map<Chart, Pair<LineChartData, List<ChartAxis>>> chartData = new HashMap<>();
        Map<ScoutMetric<Void>, Chart> chartPool = new HashMap<>();

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

            String key = getMetricKey(row);
            @MetricType int type = getMetric(teamHelper, key).getType();

            String rangeAddress = getRangeAddress(
                    first,
                    row.getCell(cell.getColumnIndex() - 1, MissingCellPolicy.CREATE_NULL_AS_BLANK));
            switch (type) {
                case MetricType.CHECKBOX:
                    cell.setCellFormula("COUNTIF(" + rangeAddress + ", TRUE) / COUNTA(" + rangeAddress + ")");
                    mCache.setCellFormat(cell, "0.00%");
                    break;
                case MetricType.COUNTER:
                    cell.setCellFormula(
                            "SUM(" + rangeAddress + ")" +
                                    " / " +
                                    "COUNT(" + rangeAddress + ")");

                    buildTeamChart(row, teamHelper, chartData, chartPool);
                    break;
                case MetricType.STOPWATCH:
                    String excludeZeros = "\"<>0\"";
                    cell.setCellFormula(
                            "IF(COUNTIF(" + rangeAddress + ", " + excludeZeros +
                                    ") = 0, 0, AVERAGEIF(" + rangeAddress + ", " + excludeZeros + "))");

                    buildTeamChart(row, teamHelper, chartData, chartPool);
                    break;
                case MetricType.SPINNER:
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
                case MetricType.HEADER:
                case MetricType.NOTE:
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
                setAxisTitle(plotArea.getValAxArray(0).addNewTitle(), "Values");
                setAxisTitle(plotArea.getCatAxArray(0).addNewTitle(), "Scouts");

                String name = getMetricForChart(xChart, chartPool).getName();
                if (!TextUtils.isEmpty(name)) xChart.setTitle(name);
            }
        }
    }

    private ScoutMetric<Void> getMetricForChart(Chart chart, Map<ScoutMetric<Void>, Chart> pool) {
        for (ScoutMetric<Void> metric : pool.keySet()) {
            if (pool.get(metric) == chart) {
                return metric;
            }
        }

        throw new IllegalStateException("Could not find chart in pool");
    }

    private ScoutMetric getMetric(TeamHelper teamHelper, String key) {
        for (Scout scout : mScouts.get(teamHelper)) {
            for (ScoutMetric metric : scout.getMetrics()) {
                if (TextUtils.equals(key, metric.getKey())) {
                    return metric;
                }
            }
        }

        throw new IllegalStateException("Key not found: " + key);
    }

    private String getRangeAddress(Cell first, Cell last) {
        return first.getAddress().toString() + ":" + last.getAddress().toString();
    }

    private void buildTeamChart(Row row,
                                TeamHelper teamHelper,
                                Map<Chart, Pair<LineChartData, List<ChartAxis>>> chartData,
                                Map<ScoutMetric<Void>, Chart> chartPool) {
        if (isUnsupportedDevice()) return;

        Sheet sheet = row.getSheet();
        int rowNum = row.getRowNum();
        int lastDataCellNum = row.getSheet().getRow(0).getLastCellNum() - 2;

        Chart chart = null;
        Pair<Integer, ScoutMetric<Void>> nearestHeader = null;

        List<Row> rows = getAdjustedList(row.getSheet());
        for (int i = row.getRowNum(); i >= 0; i--) {
            ScoutMetric metric = getMetric(teamHelper, getMetricKey(rows.get(i)));

            if (metric.getType() == MetricType.HEADER) {
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
                    if (xChart.getGraphicFrame().getAnchor().getRow1() == Constants.SINGLE_ITEM) {
                        nearestHeader = Pair.create(0, getMetricForChart(xChart, chartPool));
                        chart = xChart;
                        break chartFinder;
                    }
                }
            }

            nearestHeader = Pair.create(0, new ScoutMetric<>(null, null, MetricType.HEADER));
        }

        LineChartData data;
        if (chart == null) {
            Drawing drawing = sheet.createDrawingPatriarch();
            Integer headerIndex = nearestHeader.first + 1;
            int startChartIndex = lastDataCellNum + 3;
            chart = drawing.createChart(
                    createAnchor(drawing,
                                 getRowIndex(headerIndex, new ArrayList<>(chartData.keySet())),
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

    private int getRowIndex(int defaultIndex, List<Chart> charts) {
        if (charts.isEmpty()) return defaultIndex;

        List<ClientAnchor> anchors = new ArrayList<>();
        for (Chart chart : charts) {
            if (chart instanceof XSSFChart) {
                XSSFChart xChart = (XSSFChart) chart;
                anchors.add(xChart.getGraphicFrame().getAnchor());
            } else {
                return defaultIndex;
            }
        }

        Collections.sort(anchors, (o1, o2) -> {
            int endRow1 = o1.getRow2();
            int endRow2 = o2.getRow2();

            return endRow1 == endRow2 ? 0 : endRow1 > endRow2 ? 1 : -1;
        });

        int lastRow = anchors.get(anchors.size() - 1).getRow2();
        return defaultIndex > lastRow ? defaultIndex : lastRow;
    }

    private void setAxisTitle(CTTitle title, String text) {
        title.addNewLayout();
        title.addNewOverlay().setVal(false);

        CTTextBody textBody = title.addNewTx().addNewRich();
        textBody.addNewBodyPr();
        textBody.addNewLstStyle();

        CTTextParagraph paragraph = textBody.addNewP();
        paragraph.addNewPPr().addNewDefRPr();
        paragraph.addNewR().setT(text);
        paragraph.addNewEndParaRPr();
    }

    private void setupRow(Row row, TeamHelper teamHelper, ScoutMetric metric) {
        Cell headerCell = row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK);

        Comment comment = getComment(row, headerCell);
        comment.setString(mCache.getCreationHelper().createRichTextString(metric.getKey()));
        headerCell.setCellComment(comment);
        mCache.addTemporaryCommentCell(headerCell);

        if (metric.getType() == MetricType.HEADER) {
            headerCell.setCellStyle(mCache.getHeaderMetricRowHeaderStyle());

            int numOfScouts = mScouts.get(teamHelper).size();
            if (numOfScouts > Constants.SINGLE_ITEM) {
                int rowNum = row.getRowNum();
                row.getSheet()
                        .addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, numOfScouts));
            }
        } else {
            headerCell.setCellStyle(mCache.getRowHeaderStyle());
        }
    }

    private Comment getComment(Row row, Cell cell) {
        // When the comment box is visible, have it show in a 1x3 space
        ClientAnchor anchor = mCache.getCreationHelper().createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 1);
        anchor.setRow1(row.getRowNum());
        anchor.setRow2(row.getRowNum() + 3);

        Comment comment = row.getSheet().createDrawingPatriarch().createCellComment(anchor);
        comment.setAuthor(getString(R.string.app_name));
        return comment;
    }

    private void setRowValue(int column, ScoutMetric metric, Row row) {
        row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(metric.getName());

        Cell valueCell = row.getCell(column, MissingCellPolicy.CREATE_NULL_AS_BLANK);
        switch (metric.getType()) {
            case MetricType.CHECKBOX:
                valueCell.setCellValue((boolean) metric.getValue());
                break;
            case MetricType.COUNTER:
                valueCell.setCellValue((int) metric.getValue());
                break;
            case MetricType.SPINNER:
                SpinnerMetric spinnerMetric = (SpinnerMetric) metric;
                String selectedItem =
                        spinnerMetric.getValue().get(spinnerMetric.getSelectedValueKey());
                valueCell.setCellValue(selectedItem);
                break;
            case MetricType.NOTE:
                RichTextString note = mCache.getCreationHelper()
                        .createRichTextString(String.valueOf(metric.getValue()));
                valueCell.setCellValue(note);
                break;
            case MetricType.STOPWATCH:
                List<Long> cycles = ((StopwatchMetric) metric).getValue();

                long sum = 0;
                for (Long duration : cycles) sum += duration;
                long nanoAverage = cycles.isEmpty() ? 0 : sum / cycles.size();

                valueCell.setCellValue(TimeUnit.NANOSECONDS.toSeconds(nanoAverage));
                mCache.setCellFormat(valueCell, "#0\"s\"");
                break;
            case MetricType.HEADER:
                // Headers are skipped because they don't contain any data
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void setColumnWidths(Iterable<Sheet> sheetIterator) {
        Paint paint = new Paint();
        for (Sheet sheet : sheetIterator) {
            Row row = sheet.getRow(0);

            int numColumns = row.getLastCellNum();
            for (int i = 0; i < numColumns; i++) {
                int maxWidth = 2560;
                for (Row row1 : sheet) {
                    String value = getStringForCell(row1.getCell(i));
                    int width = (int) (paint.measureText(value) * COLUMN_WIDTH_SCALE_FACTOR);
                    if (width > maxWidth) maxWidth = width;
                }

                // We don't want the columns to be too big
                maxWidth = maxWidth < CELL_WIDTH_CEILING ? maxWidth : CELL_WIDTH_CEILING;
                sheet.setColumnWidth(i, maxWidth);
            }
        }
    }

    private String getStringForCell(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellTypeEnum()) {
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

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

                if (TextUtils.isEmpty(getStringForCell(averageCell))) continue;

                String metricKey = getMetricKey(averageRow);
                for (Cell keyCell : getAdjustedList(headerRow)) {
                    Comment keyComment = keyCell.getCellComment();
                    String key = keyComment == null ? null : keyComment.getString().toString();

                    if (TextUtils.equals(metricKey, key)) {
                        setAverageFormula(scoutSheet,
                                          row.createCell(keyCell.getColumnIndex()),
                                          averageCell);
                        continue rowIterator;
                    }
                }

                Cell keyCell = headerRow.createCell(headerRow.getLastCellNum());
                keyCell.setCellValue(averageRow.getCell(0).getStringCellValue());
                keyCell.setCellStyle(mCache.getColumnHeaderStyle());
                Comment keyComment = getComment(headerRow, keyCell);
                mCache.addTemporaryCommentCell(keyCell);
                keyComment.setString(mCache.getCreationHelper().createRichTextString(metricKey));
                keyCell.setCellComment(keyComment);

                setAverageFormula(scoutSheet,
                                  row.createCell(keyCell.getColumnIndex()),
                                  averageCell);
            }
        }

        buildAverageCharts(averageSheet);
    }

    private void buildAverageCharts(Sheet sheet) {
        if (isUnsupportedDevice()) return;

        int lastColumn = sheet.getRow(0).getLastCellNum() - 1;

        Drawing drawing = sheet.createDrawingPatriarch();
        Chart chart = drawing.createChart(
                createAnchor(drawing, sheet.getLastRowNum() + 3, 1, lastColumn + 1));

        ChartLegend legend = chart.getOrCreateLegend();
        legend.setPosition(LegendPosition.RIGHT);

        ChartDataSource<String> categorySource = DataSources.fromStringCellRange(
                sheet, new CellRangeAddress(0, 0, 1, lastColumn));

        ScatterChartData data = chart.getChartDataFactory().createScatterChartData();
        List<Row> dataRows = getAdjustedList(sheet);
        for (Row row : dataRows) {
            data.addSerie(
                    categorySource,
                    DataSources.fromNumericCellRange(
                            sheet,
                            new CellRangeAddress(row.getRowNum(),
                                                 row.getRowNum(),
                                                 1,
                                                 row.getLastCellNum() - 1)))
                    .setTitle(row.getCell(0).getStringCellValue());
        }

        ChartAxis bottomAxis = chart.getChartAxisFactory()
                .createCategoryAxis(AxisPosition.BOTTOM);
        ValueAxis leftAxis = chart.getChartAxisFactory().createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);
        chart.plot(data, bottomAxis, leftAxis);

        if (chart instanceof XSSFChart) {
            CTPlotArea plotArea = ((XSSFChart) chart).getCTChart().getPlotArea();
            setAxisTitle(plotArea.getValAxArray(0).addNewTitle(), "Values");
            setAxisTitle(plotArea.getCatAxArray(0).addNewTitle(), "Metrics");
        }
    }

    private ClientAnchor createAnchor(Drawing drawing,
                                      int startRow,
                                      int startColumn,
                                      int endColumn) {
        return drawing.createAnchor(
                0, 0, 0, 0, startColumn, startRow, endColumn, startRow + endColumn / 2);
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

    private String getMetricKey(Row row) {
        return row.getCell(0).getCellComment().getString().toString();
    }

    private <T> List<T> getAdjustedList(Iterable<T> iterator) {
        List<T> copy = new ArrayList<>();
        for (T t : iterator) copy.add(t);
        copy.remove(0);
        return copy;
    }

    private void showError(Exception e) {
        FirebaseCrash.report(e);

        String message = getString(R.string.general_error) + "\n\n" + e.getMessage();
        showToast(message);
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private static final class Cache extends TeamCache {
        private static final Object ROW_HEADER_STYLE_LOCK = new Object();
        private static final Object COLUMN_HEADER_STYLE_LOCK = new Object();

        private final List<Cell> mTemporaryCommentCells = new ArrayList<>();
        private final Map<String, Short> mFormatStyles = new ConcurrentHashMap<>();

        private CellStyle mRowHeaderStyle;
        private CellStyle mColumnHeaderStyle;

        private Workbook mWorkbook;

        public Cache(Collection<TeamHelper> teamHelpers) {
            super(teamHelpers);
        }

        public void setWorkbook(Workbook workbook) {
            mWorkbook = workbook;
        }

        public CreationHelper getCreationHelper() {
            return mWorkbook.getCreationHelper();
        }

        @Nullable
        public CellStyle getColumnHeaderStyle() {
            if (isUnsupportedDevice()) return null;

            synchronized (COLUMN_HEADER_STYLE_LOCK) {
                if (mColumnHeaderStyle == null) {
                    mColumnHeaderStyle = createColumnHeaderStyle();
                }
                return mColumnHeaderStyle;
            }
        }

        @Nullable
        public CellStyle getRowHeaderStyle() {
            if (isUnsupportedDevice()) return null;

            synchronized (ROW_HEADER_STYLE_LOCK) {
                if (mRowHeaderStyle == null) {
                    mRowHeaderStyle = createRowHeaderStyle();
                }
                return mRowHeaderStyle;
            }
        }

        @Nullable
        public CellStyle getHeaderMetricRowHeaderStyle() {
            if (isUnsupportedDevice()) return null;

            CellStyle rowHeaderStyle = createRowHeaderStyle();
            Font font = createBaseHeaderFont();
            font.setItalic(true);
            font.setFontHeightInPoints((short) 14);
            rowHeaderStyle.setFont(font);

            return rowHeaderStyle;
        }

        public void setCellFormat(Cell cell, String format) {
            if (isUnsupportedDevice()) return;

            Short cachedFormat = mFormatStyles.get(format);
            if (cachedFormat == null) {
                cachedFormat = mWorkbook.createDataFormat().getFormat(format);
                mFormatStyles.put(format, cachedFormat);
            }

            CellStyle style = mWorkbook.createCellStyle();
            style.setDataFormat(cachedFormat);
            cell.setCellStyle(style);
        }

        public void addTemporaryCommentCell(Cell cell) {
            mTemporaryCommentCells.add(cell);
        }

        public void clearTemporaryCommentCells() {
            for (Cell cell : mTemporaryCommentCells) cell.removeCellComment();
        }

        private CellStyle createColumnHeaderStyle() {
            CellStyle style = createBaseStyle();

            style.setFont(createBaseHeaderFont());
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);

            return style;
        }

        private CellStyle createRowHeaderStyle() {
            CellStyle rowHeaderStyle = createColumnHeaderStyle();
            rowHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
            return rowHeaderStyle;
        }

        private CellStyle createBaseStyle() {
            CellStyle style = mWorkbook.createCellStyle();
            style.setWrapText(true);
            return style;
        }

        private Font createBaseHeaderFont() {
            Font font = mWorkbook.createFont();
            font.setBold(true);
            return font;
        }
    }
}
