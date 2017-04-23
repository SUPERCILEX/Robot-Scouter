package com.supercilex.robotscouter.data.client.spreadsheet;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.util.TeamCache;
import com.supercilex.robotscouter.data.util.TeamHelper;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils.isUnsupportedDevice;

public final class SpreadsheetCache extends TeamCache {
    private static final Object ROW_HEADER_STYLE_LOCK = new Object();
    private static final Object COLUMN_HEADER_STYLE_LOCK = new Object();
    private static final int EXTRA_OPS = 4;

    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mProgressNotification;
    private final int mProgressMax;
    private int mCurrentProgress;

    private final Map<Cell, ScoutMetric<?>> mKeyMetrics = new HashMap<>();
    private final Map<String, Short> mFormatStyles = new HashMap<>();

    private CellStyle mRowHeaderStyle;
    private CellStyle mColumnHeaderStyle;

    private Workbook mWorkbook;

    public SpreadsheetCache(Collection<TeamHelper> teamHelpers, Context context) {
        super(teamHelpers);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mProgressMax = getTeamHelpers().size() + EXTRA_OPS;
        mProgressNotification = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(context.getString(R.string.exporting_spreadsheet_title))
                .setProgress(mProgressMax, mCurrentProgress, false)
                .setSubText("0%")
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN);
    }

    public Notification getExportNotification(String text) {
        return mProgressNotification.setContentText(text).build();
    }

    public void updateNotification(String text) {
        mCurrentProgress++;
        int percentage = Math.round((float) mCurrentProgress / (float) mProgressMax * 100);

        mProgressNotification.setProgress(mProgressMax, mCurrentProgress, false);
        mProgressNotification.setSubText(percentage + "%");
        updateNotification(R.string.exporting_spreadsheet_title, getExportNotification(text));
    }

    public void updateNotification(int id, Notification notification) {
        mNotificationManager.notify(id, notification);
    }

    public void onExportStarted() {
        mProgressNotification.setSmallIcon(android.R.drawable.stat_sys_upload);
        updateNotification(R.string.exporting_spreadsheet_title, mProgressNotification.build());
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

    public void putKeyMetric(Cell cell, ScoutMetric<?> key) {
        mKeyMetrics.put(cell, key);
    }

    public String getMetricKey(Row row) {
        return getMetricKey(row.getCell(0));
    }

    public String getMetricKey(Cell cell) {
        return getKeyMetric(cell).getKey();
    }

    public ScoutMetric<?> getKeyMetric(Cell cell) {
        return mKeyMetrics.get(cell);
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
