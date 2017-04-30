package com.supercilex.robotscouter.util;

import android.content.Context;
import android.content.res.Configuration;

public enum ViewUtils {;
    public static boolean isTabletMode(Context context) {
        Configuration config = context.getResources().getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return size == Configuration.SCREENLAYOUT_SIZE_LARGE
                && config.orientation == Configuration.ORIENTATION_LANDSCAPE
                || size > Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
