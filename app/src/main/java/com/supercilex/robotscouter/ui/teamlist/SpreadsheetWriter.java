package com.supercilex.robotscouter.ui.teamlist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Pair;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.SpinnerMetric;
import com.supercilex.robotscouter.data.util.Scouts;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;
import com.supercilex.robotscouter.util.Constants;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import pub.devrel.easypermissions.EasyPermissions;

public class SpreadsheetWriter implements Callable<Void>, OnSuccessListener<Map<TeamHelper, List<List<ScoutMetric>>>> {
    public static final String[] PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String EXPORT_FOLDER_NAME = "Robot Scouter team exports/";
    private static final String FILE_EXTENSION = ".xlsx";
    private static final int COLUMN_WIDTH_SCALE_FACTOR = 46;
    private static final int CELL_WIDTH_CEILING = 7500;

    private Context mContext;
    private List<TeamHelper> mTeamHelpers;

    private Map<TeamHelper, List<List<ScoutMetric>>> mScouts;

    protected SpreadsheetWriter(Context context, List<TeamHelper> teamHelpers) {
        mContext = context;
        mTeamHelpers = teamHelpers;

        Collections.sort(mTeamHelpers);
    }

    /**
     * @return true if an export was attempted, false otherwise
     */
    public static boolean writeAndShareTeams(Fragment fragment, List<TeamHelper> teamHelpers) {
        if (teamHelpers.isEmpty()) return false;

        if (!EasyPermissions.hasPermissions(fragment.getContext(), PERMS)) {
            EasyPermissions.requestPermissions(
                    fragment,
                    fragment.getString(R.string.write_storage_rationale),
                    8653,
                    PERMS);
            return false;
        }

        AsyncTaskExecutor.execute(
                new SpreadsheetWriter(fragment.getContext().getApplicationContext(),
                                      new ArrayList<>(teamHelpers)));

        return true;
    }

    @Override
    public Void call() throws Exception {
        Scouts.getAll(mTeamHelpers)
                .addOnSuccessListener(AsyncTaskExecutor.getCurrentExecutor(this), this);
        return null;
    }

    @Override
    public void onSuccess(Map<TeamHelper, List<List<ScoutMetric>>> scouts) {
        mScouts = scouts;

        Uri spreadsheetUri = getFileUri();
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

        buildTeamAveragesSheet(workbook.createSheet("Team averages"));

        for (TeamHelper teamHelper : mTeamHelpers) {
            Sheet teamSheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(teamHelper.getFormattedName()));
            teamSheet.createFreezePane(1, 1);
            buildTeamSheet(teamHelper, teamSheet);
        }

        setColumnWidths(workbook.sheetIterator());

        return workbook;
    }

    private void buildTeamAveragesSheet(Sheet allTeamsSheet) {
        Row header = allTeamsSheet.createRow(0);
        CellStyle rowHeaderStyle = getRowHeaderStyle(allTeamsSheet.getWorkbook());

        for (int i = 0, rowNum = 1; i < mTeamHelpers.size(); i++, rowNum++) {
            TeamHelper helper = mTeamHelpers.get(i);

            Row row = allTeamsSheet.createRow(rowNum);
            row.createCell(0).setCellValue(helper.getFormattedName());

            List<List<ScoutMetric>> scouts = mScouts.get(helper);
            for (List<ScoutMetric> scout : scouts) {
                for (int j = 0, column = 1; j < scout.size(); j++, column = j + 1) {
                    ScoutMetric metric = scout.get(j);

                    if (metric.getType() == MetricType.NOTE) continue;

                    Cell cell = header.getCell(column);
                    if (cell == null) {
                        cell = header.createCell(column);
                        cell.setCellValue(metric.getName());
                        cell.setCellStyle(rowHeaderStyle);
                    } else {
                        if (!cell.getStringCellValue().equals(metric.getName())) {
                            Iterator<Cell> iterator = header.cellIterator();
                            boolean exists = false;
                            for (int k = 0; iterator.hasNext(); k++) {
                                if (metric.getName().equals(iterator.next().getStringCellValue())) {
                                    column = k;
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                column = header.getLastCellNum();
                                header.createCell(column).setCellValue(metric.getName());
                            }
                        }
                    }

                    setRowValue(column, getAverageScoutMetric(scouts, metric), row);
                }
            }
        }
    }

    private ScoutMetric getAverageScoutMetric(List<List<ScoutMetric>> scouts,
                                              ScoutMetric metricToFind) {
        int totalNumMetrics = 0, booleanCounter = 0, numericalCounter = 0;

        List<Integer> numOfSelections = null;
        if (metricToFind.getType() == MetricType.SPINNER) {
            List<String> spinnerValues = ((SpinnerMetric) metricToFind).getValue();
            numOfSelections = new ArrayList<>(Collections.nCopies(spinnerValues.size(), 0));
        }

        for (List<ScoutMetric> scout : scouts) {
            for (ScoutMetric metric : scout) {
                if (metric.getType() == metricToFind.getType()
                        && metric.getName().equals(metricToFind.getName())) {
                    totalNumMetrics++;

                    switch (metric.getType()) {
                        case MetricType.CHECKBOX:
                            if (metric.getValue().equals(true)) booleanCounter++;
                            break;
                        case MetricType.COUNTER:
                            numericalCounter += (int) metric.getValue();
                            break;
                        case MetricType.SPINNER:
                            SpinnerMetric spinnerMetric = (SpinnerMetric) metric;
                            int index = spinnerMetric.getSelectedValueIndex();
                            numOfSelections.set(index, numOfSelections.get(index) + 1);
                            break;
                        case MetricType.NOTE:
                            return null;
                    }
                }
            }
        }

        switch (metricToFind.getType()) {
            case MetricType.CHECKBOX:
                return new ScoutMetric<>(metricToFind.getName(),
                                         booleanCounter >= totalNumMetrics / 2.0,
                                         MetricType.CHECKBOX);
            case MetricType.COUNTER:
                return new ScoutMetric<>(metricToFind.getName(),
                                         numericalCounter / totalNumMetrics,
                                         MetricType.COUNTER);
            case MetricType.SPINNER:
                Pair<Integer, Integer> max = new Pair<>(0, 0);
                for (int i = 0; i < numOfSelections.size(); i++) {
                    Integer count = numOfSelections.get(i);
                    if (count > max.first) {
                        max = new Pair<>(count, i); // NOPMD
                    }
                }
                return new SpinnerMetric(metricToFind.getName(),
                                         ((SpinnerMetric) metricToFind).getValue(),
                                         max.second);
            case MetricType.NOTE:
            default:
                return null;
        }
    }

    private void buildTeamSheet(TeamHelper teamHelper, Sheet teamSheet) {
        List<List<ScoutMetric>> scouts = mScouts.get(teamHelper);

        Workbook workbook = teamSheet.getWorkbook();
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle rowHeaderStyle = getRowHeaderStyle(workbook);

        Row header = teamSheet.createRow(0);
        for (int i = 0; i < scouts.size() + 1; i++) {
            if (i == 0) {
                header.createCell(0); // Create empty top left corner cell
                continue;
            }

            Cell cell = header.createCell(i);
            cell.setCellValue("Scout " + i);
            cell.setCellStyle(headerStyle);
        }


        for (int i = 0, column = 1; i < scouts.size(); i++, column++) {
            List<ScoutMetric> scout = scouts.get(i);

            columnIterator:
            for (int j = 0, rowNum = 1; j < scout.size(); j++, rowNum++) {
                ScoutMetric metric = scout.get(j);

                Row row = teamSheet.getRow(rowNum);
                if (row == null) {
                    createRowAndSetValue(teamSheet.createRow(rowNum),
                                         metric,
                                         column,
                                         rowHeaderStyle);
                } else {
                    List<Row> rows = copyIterator(teamSheet.rowIterator());

                    for (int k = 0; k < rows.size(); k++) {
                        if (k == 0) continue; // Skip header row

                        Row row1 = rows.get(k);
                        if (row1.getCell(0).getStringCellValue().equals(metric.getName())
                                && row1.getRowNum() == scout.indexOf(metric) + 1) {
                            setRowValue(column, metric, row1);
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
        headerCell.setCellStyle(headerStyle);

        setRowValue(column, metric, row);
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
                RichTextString note = new XSSFRichTextString(String.valueOf(metric.getValue()));
                valueCell.setCellValue(note);
                break;
        }
    }

    private void setColumnWidths(Iterator<Sheet> sheetIterator) {
        Paint paint = new Paint();
        while (sheetIterator.hasNext()) {
            Sheet sheet = sheetIterator.next();
            Row row = sheet.getRow(0);

            int numColumns = row.getLastCellNum();
            for (int i = 0; i < numColumns; i++) {
                int maxWidth = 2560;
                Iterator<Row> rowIterator = sheet.rowIterator();
                while (rowIterator.hasNext()) {
                    String value = getStringForCell(rowIterator.next().getCell(i));
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
}
