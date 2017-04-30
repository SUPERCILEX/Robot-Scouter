package com.supercilex.robotscouter.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

public enum ViewUtils {
    INSTANCE;

    private static Resources sResources;

    public static void init(Context context) {
        sResources = context.getResources();
    }

    public static boolean isTabletMode() {
        Configuration config = sResources.getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return size == Configuration.SCREENLAYOUT_SIZE_LARGE
                && config.orientation == Configuration.ORIENTATION_LANDSCAPE
                || size > Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
