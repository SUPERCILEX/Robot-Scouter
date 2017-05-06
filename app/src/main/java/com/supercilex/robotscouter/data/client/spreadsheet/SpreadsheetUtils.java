package com.supercilex.robotscouter.data.client.spreadsheet;

import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.util.TeamHelper;

import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.formula.eval.NumberEval;
import org.apache.poi.ss.formula.eval.ValueEval;
import org.apache.poi.ss.formula.functions.Countif;
import org.apache.poi.ss.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.functions.Sumif;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Chart;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum SpreadsheetUtils {;
    public static final FreeRefFunction AVERAGEIF = new FreeRefFunction() {
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

    private static final String DEVICE_MODEL = (Build.MANUFACTURER.toUpperCase(Locale.ROOT) + "-" +
            Build.MODEL.toUpperCase(Locale.ROOT)).replace(' ', '-');
    private static final List<String> UNSUPPORTED_DEVICES =
            Collections.unmodifiableList(Arrays.asList("SAMSUNG-SM-N900A"));

    private static final int MAX_SHEET_LENGTH = 31;
    private static final int COLUMN_WIDTH_SCALE_FACTOR = 46;
    private static final int CELL_WIDTH_CEILING = 7500;

    public static boolean isUnsupportedDevice() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || UNSUPPORTED_DEVICES.contains(DEVICE_MODEL);
    }

    public static String getStringForCell(Cell cell) {
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

    public static String getCellRangeAddress(Cell first, Cell last) {
        return first.getAddress().toString() + ":" + last.getAddress().toString();
    }

    public static String getSafeSheetName(Workbook workbook, TeamHelper teamHelper) {
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

    public static void autoFitColumnWidths(Iterable<Sheet> sheetIterator) {
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

    /**
     * @return A list with its first item removed to take into account header rows and columns.
     */
    public static <T> List<T> getAdjustedList(Iterable<T> iterator) {
        List<T> copy = new ArrayList<>();
        for (T t : iterator) copy.add(t);
        copy.remove(0);
        return copy;
    }

    public static ScoutMetric getMetricForScouts(List<Scout> scouts, String key) {
        for (Scout scout : scouts) {
            for (ScoutMetric metric : scout.getMetrics()) {
                if (TextUtils.equals(key, metric.getKey())) {
                    return metric;
                }
            }
        }

        throw new IllegalStateException("Key not found: " + key);
    }

    public static ScoutMetric<Void> getMetricForChart(Chart chart,
                                                      Map<ScoutMetric<Void>, Chart> pool) {
        for (ScoutMetric<Void> metric : pool.keySet()) {
            if (pool.get(metric) == chart) {
                return metric;
            }
        }

        throw new IllegalStateException("Chart not found in pool");
    }

    public static int getChartRowIndex(int defaultIndex, List<Chart> charts) {
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

    public static void setChartAxisTitle(CTTitle title, String text) {
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

    public static ClientAnchor createChartAnchor(Drawing drawing,
                                                 int startRow,
                                                 int startColumn,
                                                 int endColumn) {
        return drawing.createAnchor(
                0, 0, 0, 0, startColumn, startRow, endColumn, startRow + endColumn / 2);
    }

    public static void showError(Context context, Exception e) {
        FirebaseCrash.report(e);

        String message = context.getString(R.string.general_error) + "\n\n" + e.getMessage();
        showToast(context, message);
    }

    public static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
