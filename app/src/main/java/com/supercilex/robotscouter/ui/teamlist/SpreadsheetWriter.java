package com.supercilex.robotscouter.ui.teamlist;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.Size;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

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

import pub.devrel.easypermissions.EasyPermissions;

public class SpreadsheetWriter implements OnSuccessListener<Map<TeamHelper, List<Scout>>> {
    public static final String[] PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String EXPORT_FOLDER_NAME = "Robot Scouter team exports/";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final int COLUMN_WIDTH_SCALE_FACTOR = 46;
    private static final int CELL_WIDTH_CEILING = 7500;

    private Context mContext;
    private ProgressDialogManager mProgressDialog;
    private List<TeamHelper> mTeamHelpers;

    private Map<TeamHelper, List<Scout>> mScouts;
    private CreationHelper mCreationHelper;

    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    protected SpreadsheetWriter(Fragment fragment, @Size(min = 1) List<TeamHelper> teamHelpers) {
        mContext = fragment.getContext().getApplicationContext();
        mTeamHelpers = teamHelpers;
        mProgressDialog = ProgressDialogManager.show(fragment.getActivity());

        Collections.sort(mTeamHelpers);
        Scouts.getAll(mTeamHelpers).addOnSuccessListener(new AsyncTaskExecutor(), this);
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

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("application/vnd.ms-excel");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, spreadsheetUri);
        mContext.startActivity(Intent.createChooser(sharingIntent, getShareTitle())
                                       .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                       .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
    }

    private String getShareTitle() {
        return mContext.getResources()
                .getQuantityString(R.plurals.share_spreadsheet_title,
                                   mTeamHelpers.size(),
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

    private File writeFile(File robotScouterFolder) {
        FileOutputStream stream = null;
        try {
            File absoluteFile = new File(robotScouterFolder, getFullyQualifiedFileName(null));
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
        return middleMan == null ? getTeamNames() + FILE_EXTENSION : getTeamNames() + middleMan + FILE_EXTENSION;
    }

    private String getTeamNames() {
        String teamName;
        if (mTeamHelpers.size() == Constants.SINGLE_ITEM) {
            teamName = mTeamHelpers.get(0).getFormattedName();
        } else {
            StringBuilder names = new StringBuilder(4 * mTeamHelpers.size());
            for (int i = 0,
                 size = mTeamHelpers.size(); i < size; i++) {
                names.append(mTeamHelpers.get(i).getTeam().getNumber());
                if (i < size - 1) names.append(", ");
            }
            teamName = names.toString();
        }
        return teamName;
    }

    private Workbook getWorkbook() {
        setApacheProperties();

        Workbook workbook = new XSSFWorkbook();
        mCreationHelper = workbook.getCreationHelper();

//        Sheet averageSheet = null;
//        if (mTeamHelpers.size() > Constants.SINGLE_ITEM) {
//            averageSheet = workbook.createSheet("Team Averages");
//            averageSheet.createFreezePane(1, 1);
//        }

        for (TeamHelper teamHelper : mTeamHelpers) {
            Sheet teamSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(teamHelper.getFormattedName()));
            teamSheet.createFreezePane(1, 1);
            buildTeamSheet(teamHelper, teamSheet);
        }

//        if (averageSheet != null) {
//            buildTeamAveragesSheet(averageSheet);
//        }

        setColumnWidths(workbook);

        return workbook;
    }

    private void removeTemporaryComments(Sheet sheet) {
        for (Row row : sheet) row.getCell(0).removeCellComment();
    }

    private void buildTeamSheet(TeamHelper teamHelper, Sheet teamSheet) {
        List<Scout> scouts = mScouts.get(teamHelper);

        Workbook workbook = teamSheet.getWorkbook();
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle rowHeaderStyle = getRowHeaderStyle(workbook);

        List<Integer> excludedAverageRows = new ArrayList<>();

        Row header = teamSheet.createRow(0);
        header.createCell(0); // Create empty top left corner cell
        for (int i = 0, column = 1; i < scouts.size(); i++, column++) {
            Scout scout = scouts.get(i);
            List<ScoutMetric> metrics = scout.getMetrics();

            Cell cell = header.createCell(column);
            String name = scout.getName();
            cell.setCellValue(TextUtils.isEmpty(name) ? "Scout " + column : name);
            cell.setCellStyle(headerStyle);

            columnIterator:
            for (int j = 0, rowNum = 1; j < metrics.size(); j++, rowNum++) {
                ScoutMetric metric = metrics.get(j);

                if (metric.getType() == MetricType.HEADER) continue; // No data here

                Row row = teamSheet.getRow(rowNum);
                if (row == null) {
                    createRowAndSetValue(teamSheet.createRow(rowNum),
                                         metric,
                                         column,
                                         rowHeaderStyle);
                    if (metric.getType() == MetricType.NOTE) excludedAverageRows.add(rowNum);
                } else {
                    List<Row> rows = copyIterator(teamSheet.rowIterator());

                    for (int k = 0; k < rows.size(); k++) {
                        if (k == 0) continue; // Skip header row

                        Row row1 = rows.get(k);
                        Cell cell1 = row1.getCell(0);
                        String rowKey = cell1.getCellComment().getString().toString();
                        if (TextUtils.equals(rowKey, metric.getRef().getKey())) {
                            setRowValue(column, metric, row1);

                            if (TextUtils.isEmpty(cell1.getStringCellValue())) {
                                cell1.setCellValue(metric.getName());
                            }

                            continue columnIterator;
                        }
                    }

                    teamSheet.shiftRows(rowNum, teamSheet.getLastRowNum(), 1);
                    createRowAndSetValue(teamSheet.createRow(rowNum),
                                         metric,
                                         column,
                                         rowHeaderStyle);
                }
            }
        }


        if (scouts.size() > Constants.SINGLE_ITEM) {
            buildAverageCells(teamSheet, headerStyle, excludedAverageRows);
        }

        removeTemporaryComments(teamSheet);
    }

    private void buildAverageCells(Sheet sheet, CellStyle headerStyle, List<Integer> excludedRows) {
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
                cell.setCellStyle(headerStyle);
                continue;
            }

            Cell first = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String rangeAddress =
                    getRangeAddress(first, row.getCell(cell.getColumnIndex() - 1,
                                                       Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
            switch (first.getCellTypeEnum()) {
                case NUMERIC:
                    cell.setCellFormula(
                            "SUM(" + rangeAddress + ")" +
                                    " / " +
                                    "COUNT(" + rangeAddress + ")");
                    break;
                case BOOLEAN:
                    cell.setCellFormula(
                            "IF(" +
                                    "COUNTIF(" + rangeAddress + ", TRUE)" +
                                    " >= " +
                                    "COUNTIF(" + rangeAddress + ", FALSE)" +
                                    ", TRUE, FALSE)");
                    break;
                case STRING:
                    if (excludedRows.contains(i)) break;

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
            }
        }
    }

    private String getRangeAddress(Cell first, Cell last) {
        return first.getAddress().toString() + ":" + last.getAddress().toString();
    }

    private CellStyle getRowHeaderStyle(Workbook workbook) {
        CellStyle rowHeaderStyle = createHeaderStyle(workbook);
        rowHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        return rowHeaderStyle;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(font);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return headerStyle;
    }

    private void createRowAndSetValue(Row row,
                                      ScoutMetric metric,
                                      int column,
                                      CellStyle headerStyle) {
        Cell headerCell = row.createCell(0);
        headerCell.setCellValue(metric.getName());

        Comment comment = getComment(row, headerCell);
        comment.setString(mCreationHelper.createRichTextString(metric.getRef().getKey()));

        headerCell.setCellComment(comment);
        headerCell.setCellStyle(headerStyle);

        setRowValue(column, metric, row);
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
        Cell valueCell = row.createCell(column);
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
                StopwatchMetric stopwatchMetric = (StopwatchMetric) metric;
                long sum = 0;
                List<Long> cycles = stopwatchMetric.getValue();
                for (Long duration : cycles) {
                    sum += duration;
                }
                valueCell.setCellValue(cycles.isEmpty() ? sum : sum / cycles.size());
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

    private <T> List<T> copyIterator(Iterator<T> iterator) {
        List<T> copy = new ArrayList<>();
        while (iterator.hasNext()) copy.add(iterator.next());
        return copy;
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
