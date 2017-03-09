package com.supercilex.robotscouter.ui.teamlist;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.Size;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;
import com.supercilex.robotscouter.data.model.metrics.StopwatchMetric;
import com.supercilex.robotscouter.data.util.Scouts;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;
import com.supercilex.robotscouter.util.Constants;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.EasyPermissions;

import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy;

public final class SpreadsheetWriter implements OnSuccessListener<Map<TeamHelper, List<Scout>>> {
    public static final String[] PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String EXPORT_FOLDER_NAME = "Robot Scouter/";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final String UNSUPPORTED_FILE_EXTENSION = ".xls";
    private static final int COLUMN_WIDTH_SCALE_FACTOR = 46;
    private static final int CELL_WIDTH_CEILING = 7500;
    private static final String MIME_TYPE_MS_EXCEL = "application/vnd.ms-excel";

    private Context mContext;
    private ProgressDialogManager mProgressDialog;

    private Map<TeamHelper, List<Scout>> mScouts;
    private List<Cell> mTemporaryCommentCells = new ArrayList<>();
    private CreationHelper mCreationHelper;

    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private SpreadsheetWriter(Fragment fragment, @Size(min = 1) List<TeamHelper> teamHelpers) {
        mContext = fragment.getContext().getApplicationContext();
        mProgressDialog = ProgressDialogManager.show(fragment.getActivity());

        Collections.sort(teamHelpers);
        Scouts.getAll(teamHelpers).addOnSuccessListener(new AsyncTaskExecutor(), this);
    }

    /**
     * @return true if an export was attempted, false otherwise
     */
    public static boolean writeAndShareTeams(Fragment fragment,
                                             @Size(min = 1) List<TeamHelper> teamHelpers) {
        if (teamHelpers.isEmpty()) return false;

        if (!EasyPermissions.hasPermissions(fragment.getContext(), PERMS)) {
            EasyPermissions.requestPermissions(
                    fragment,
                    fragment.getString(R.string.write_storage_rationale),
                    8653,
                    PERMS);
            return false;
        }

        //noinspection MissingPermission
        new SpreadsheetWriter(fragment, new ArrayList<>(teamHelpers));

        return true;
    }

    @Override
    public void onSuccess(Map<TeamHelper, List<Scout>> scouts) {
        mScouts = scouts;

        Uri spreadsheetUri = getFileUri();
        mProgressDialog.dismiss();
        if (spreadsheetUri == null) return;

        Intent sharingIntent = new Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            sharingIntent.setAction(Intent.ACTION_VIEW)
                    .setDataAndType(spreadsheetUri, MIME_TYPE_MS_EXCEL);
            try {
                mContext.startActivity(sharingIntent);
            } catch (ActivityNotFoundException e) {
                sharingIntent.setDataAndType(spreadsheetUri, "*/*");
                mContext.startActivity(sharingIntent);
            }
        } else {
            sharingIntent.setAction(Intent.ACTION_SEND)
                    .setType(MIME_TYPE_MS_EXCEL)
                    .putExtra(Intent.EXTRA_STREAM, spreadsheetUri);
            mContext.startActivity(Intent.createChooser(sharingIntent, getShareTitle())
                                           .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        }
    }

    private String getShareTitle() {
        return mContext.getResources()
                .getQuantityString(R.plurals.share_spreadsheet_title,
                                   mScouts.size(),
                                   getTeamNames());
    }

    private Uri getFileUri() {
        if (!isExternalStorageWritable()) return null;
        String pathname =
                Environment.getExternalStorageDirectory().toString() + "/" + EXPORT_FOLDER_NAME;
        File robotScouterFolder = new File(pathname);
        if (!robotScouterFolder.exists() && !robotScouterFolder.mkdirs()) return null;

        File file = writeFile(robotScouterFolder);
        return file == null ? null : Uri.fromFile(file);
    }

    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    @Nullable
    private File writeFile(File robotScouterFolder) {
        FileOutputStream stream = null;
        File absoluteFile = new File(robotScouterFolder, getFullyQualifiedFileName(null));
        try {
            for (int i = 1; true; i++) {
                if (absoluteFile.createNewFile()) {
                    break;
                } else { // File already exists
                    absoluteFile = new File(robotScouterFolder, // NOPMD
                                            getFullyQualifiedFileName(" (" + i + ")"));
                }
            }

            stream = new FileOutputStream(absoluteFile);
            getWorkbook().write(stream);

            return absoluteFile;
        } catch (IOException e) {
            FirebaseCrash.report(e);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.general_error, Toast.LENGTH_SHORT).show();
                }
            });
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
        return middleMan == null ? getTeamNames() + extension : getTeamNames() + middleMan + extension;
    }

    private String getTeamNames() {
        String teamName;
        int size = mScouts.size();
        List<TeamHelper> teamHelpers = getTeamHelpers();

        if (size == Constants.SINGLE_ITEM) {
            teamName = teamHelpers.get(0).toString();
        } else {
            StringBuilder names = new StringBuilder(4 * size);
            for (int i = 0; i < size; i++) {
                names.append(teamHelpers.get(i).getTeam().getNumber());
                if (i < size - 1) names.append(", ");
            }
            teamName = names.toString();
        }

        return teamName;
    }

    private List<TeamHelper> getTeamHelpers() {
        ArrayList<TeamHelper> helpers = new ArrayList<>(mScouts.keySet());
        Collections.sort(helpers);
        return helpers;
    }

    private Workbook getWorkbook() {
        setApacheProperties();

        Workbook workbook;
        if (isUnsupportedDevice()) {
            workbook = new HSSFWorkbook();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.unsupported_device, Toast.LENGTH_SHORT)
                            .show();
                }
            });
        } else {
            workbook = new XSSFWorkbook();
        }
        mCreationHelper = workbook.getCreationHelper();

        Sheet averageSheet = null;
        if (mScouts.size() > Constants.SINGLE_ITEM) {
            averageSheet = workbook.createSheet("Team Averages");
            averageSheet.createFreezePane(1, 1);
        }

        List<TeamHelper> teamHelpers = getTeamHelpers();
        for (TeamHelper teamHelper : teamHelpers) {
            Sheet teamSheet = workbook.createSheet(getSafeName(workbook, teamHelper));
            teamSheet.createFreezePane(1, 1);
            buildTeamSheet(teamHelper, teamSheet);
        }

        if (averageSheet != null) buildTeamAveragesSheet(averageSheet);

        setColumnWidths(workbook);

        for (Cell cell : mTemporaryCommentCells) cell.removeCellComment();

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
            }
        }

        return safeName;
    }

    private void buildTeamSheet(TeamHelper teamHelper, Sheet teamSheet) {
        List<Scout> scouts = mScouts.get(teamHelper);

        Workbook workbook = teamSheet.getWorkbook();

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
            cell.setCellStyle(createHeaderStyle(workbook));

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
                        String rowKey = cell1.getCellComment().getString().toString();
                        if (TextUtils.equals(rowKey, metric.getKey())) {
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

        Iterator<Row> rowIterator = sheet.rowIterator();
        for (int i = 0; rowIterator.hasNext(); i++) {
            Row row = rowIterator.next();
            Cell cell = row.createCell(farthestColumn);
            if (i == 0) {
                cell.setCellValue(mContext.getString(R.string.average));
                cell.setCellStyle(createHeaderStyle(sheet.getWorkbook()));
                continue;
            }

            String key = row.getCell(0).getCellComment().getString().toString();
            @MetricType int type = getMetricType(teamHelper, key);

            String rangeAddress = getRangeAddress(
                    row.getCell(1, MissingCellPolicy.CREATE_NULL_AS_BLANK),
                    row.getCell(cell.getColumnIndex() - 1,
                                MissingCellPolicy.CREATE_NULL_AS_BLANK));
            switch (type) {
                case MetricType.CHECKBOX:
                    cell.setCellFormula(
                            "IF(" +
                                    "COUNTIF(" + rangeAddress + ", TRUE)" +
                                    " >= " +
                                    "COUNTIF(" + rangeAddress + ", FALSE)" +
                                    ", TRUE, FALSE)");
                    break;
                case MetricType.COUNTER:
                case MetricType.STOPWATCH:
                    cell.setCellFormula(
                            "SUM(" + rangeAddress + ")" +
                                    " / " +
                                    "COUNT(" + rangeAddress + ")");
                    break;
                case MetricType.SPINNER:
                    cell.setCellFormula(
                            "ARRAYFORMULA(" +
                                    "INDEX(" + rangeAddress + ", " +
                                    "MATCH(" +
                                    "MAX(" +
                                    "COUNTIF(" + rangeAddress + ", " + rangeAddress + ")" +
                                    "), " +
                                    "COUNTIF(" + rangeAddress + ", " + rangeAddress + ")" +
                                    ", 0)))");
                    break;
                case MetricType.HEADER:
                case MetricType.NOTE:
                    // Nothing to average
                    break;
            }
        }
    }

    @MetricType
    private int getMetricType(TeamHelper teamHelper, String key) {
        for (Scout scout : mScouts.get(teamHelper)) {
            for (ScoutMetric metric : scout.getMetrics()) {
                if (key.equals(metric.getKey())) {
                    return metric.getType();
                }
            }
        }

        throw new IllegalStateException("Key not found: " + key);
    }

    private String getRangeAddress(Cell first, Cell last) {
        return first.getAddress().toString() + ":" + last.getAddress().toString();
    }

    @Nullable
    private CellStyle createRowHeaderStyle(Workbook workbook) {
        CellStyle rowHeaderStyle = createHeaderStyle(workbook);
        if (rowHeaderStyle != null) rowHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        return rowHeaderStyle;
    }

    @Nullable
    private CellStyle createHeaderStyle(Workbook workbook) {
        if (isUnsupportedDevice()) return null;

        Font font = createBaseHeaderFont(workbook);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(font);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return headerStyle;
    }

    @Nullable
    private Font createBaseHeaderFont(Workbook workbook) {
        if (isUnsupportedDevice()) return null;

        Font font = workbook.createFont();
        font.setBold(true);
        return font;
    }

    private void setupRow(Row row, TeamHelper teamHelper, ScoutMetric metric) {
        Cell headerCell = row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK);

        Comment comment = getComment(row, headerCell);
        comment.setString(mCreationHelper.createRichTextString(metric.getKey()));
        headerCell.setCellComment(comment);
        mTemporaryCommentCells.add(headerCell);

        Sheet sheet = row.getSheet();
        Workbook workbook = sheet.getWorkbook();
        CellStyle headerStyle = createRowHeaderStyle(workbook);
        if (headerStyle != null && metric.getType() == MetricType.HEADER) {
            Font font = createBaseHeaderFont(workbook);
            if (font != null) {
                font.setItalic(true);
                font.setFontHeightInPoints((short) 14);
                headerStyle.setFont(font);
            }

            int rowNum = row.getRowNum();
            int numOfScouts = mScouts.get(teamHelper).size();
            if (numOfScouts > Constants.SINGLE_ITEM) {
                sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, numOfScouts));
            }
        }
        headerCell.setCellStyle(headerStyle);
    }

    private Comment getComment(Row row, Cell cell) {
        // When the comment box is visible, have it show in a 1x3 space
        ClientAnchor anchor = mCreationHelper.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 1);
        anchor.setRow1(row.getRowNum());
        anchor.setRow2(row.getRowNum() + 3);

        Comment comment = row.getSheet().createDrawingPatriarch().createCellComment(anchor);
        comment.setAuthor(mContext.getString(R.string.app_name));
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
                        spinnerMetric.getValue().get(spinnerMetric.getSelectedValueIndex());
                valueCell.setCellValue(selectedItem);
                break;
            case MetricType.NOTE:
                RichTextString note = mCreationHelper.createRichTextString(String.valueOf(metric.getValue()));
                valueCell.setCellValue(note);
                break;
            case MetricType.STOPWATCH:
                List<Long> cycles = ((StopwatchMetric) metric).getValue();

                long sum = 0;
                for (Long duration : cycles) sum += duration;
                long nanoAverage = cycles.isEmpty() ? 0 : sum / cycles.size();

                valueCell.setCellValue(TimeUnit.NANOSECONDS.toSeconds(nanoAverage));
                break;
            case MetricType.HEADER:
                // Headers are skipped because they don't contain any data
                break;
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

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle rowHeaderStyle = createRowHeaderStyle(workbook);

        List<Sheet> scoutSheets = getAdjustedList(workbook);
        for (int i = 0; i < scoutSheets.size(); i++) {
            Sheet scoutSheet = scoutSheets.get(i);
            Row row = averageSheet.createRow(i + 1);
            Cell rowHeaderCell = row.createCell(0);
            rowHeaderCell.setCellValue(scoutSheet.getSheetName());
            rowHeaderCell.setCellStyle(rowHeaderStyle);

            List<Row> metricsRows = getAdjustedList(scoutSheet);
            rowIterator:
            for (Row averageRow : metricsRows) {
                Cell averageCell = averageRow.getCell(averageRow.getLastCellNum() - 1);

                if (TextUtils.isEmpty(getStringForCell(averageCell))) continue;

                Cell metricCell = averageRow.getCell(0);
                String metricKey = metricCell.getCellComment().getString().toString();

                for (Cell keyCell : getAdjustedList(headerRow)) {
                    Comment keyComment = keyCell.getCellComment();
                    String key = keyComment == null ? null : keyComment.getString().toString();

                    if (metricKey.equals(key)) {
                        setAverageFormula(scoutSheet,
                                          row.createCell(keyCell.getColumnIndex()),
                                          averageCell);
                        continue rowIterator;
                    }
                }

                Cell keyCell = headerRow.createCell(headerRow.getLastCellNum());
                keyCell.setCellValue(metricCell.getStringCellValue());
                keyCell.setCellStyle(headerStyle);
                Comment keyComment = getComment(headerRow, keyCell);
                mTemporaryCommentCells.add(keyCell);
                keyComment.setString(mCreationHelper.createRichTextString(metricKey));
                keyCell.setCellComment(keyComment);

                setAverageFormula(scoutSheet,
                                  row.createCell(keyCell.getColumnIndex()),
                                  averageCell);
            }
        }
    }

    private void setAverageFormula(Sheet scoutSheet, Cell valueCell, Cell averageCell) {
        valueCell.setCellFormula("'" + scoutSheet.getSheetName() + "'!" + averageCell.getAddress());
    }

    private <T> List<T> getAdjustedList(Iterable<T> iterator) {
        List<T> copy = new ArrayList<>();
        for (T t : iterator) copy.add(t);
        copy.remove(0);
        return copy;
    }

    private boolean isUnsupportedDevice() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    private void setApacheProperties() {
        System.setProperty("org.apache.poi.javax.xml.stream.XMLInputFactory",
                           "com.fasterxml.aalto.stax.InputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory",
                           "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory",
                           "com.fasterxml.aalto.stax.EventFactoryImpl");
    }

    private static class ProgressDialogManager implements Application.ActivityLifecycleCallbacks {
        private Application mApplication;
        private WeakReference<ProgressDialog> mProgressDialog;

        public ProgressDialogManager(Activity activity) {
            mApplication = activity.getApplication();

            mApplication.registerActivityLifecycleCallbacks(this);
            initProgressDialog(activity);
        }

        private static ProgressDialogManager show(Activity activity) {
            return new ProgressDialogManager(activity);
        }

        private void initProgressDialog(Activity activity) {
            mProgressDialog = new WeakReference<>(ProgressDialog.show(
                    activity,
                    "",
                    activity.getString(R.string.progress_dialog_loading),
                    true));
        }

        public void dismiss() {
            internalDismiss();
            mApplication.unregisterActivityLifecycleCallbacks(this);
        }

        private void internalDismiss() {
            ProgressDialog dialog = mProgressDialog.get();
            if (dialog != null) {
                dialog.dismiss();
                mProgressDialog = new WeakReference<>(null);
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (mProgressDialog.get() == null) initProgressDialog(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            internalDismiss();
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }
}
