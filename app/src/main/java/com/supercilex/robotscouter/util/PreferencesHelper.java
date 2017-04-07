package com.supercilex.robotscouter.util;

import android.content.Context;

import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;

public final class PreferencesHelper {
    private static final String TEAM_LIST_ACTIVITY_PREF_NAME = TeamListActivity.class.getName();
    private static final String EXPORT_PREF_NAME = "spreadsheet_export";

    private static final String HAS_SHOWN_TUTORIAL = "has_shown_tutorial";
    private static final String HAS_SHOWN_FAB_TUTORIAL = HAS_SHOWN_TUTORIAL + "_fab";
    private static final String HAS_SHOWN_SIGN_IN_TUTORIAL = HAS_SHOWN_TUTORIAL + "_sign_in";

    private PreferencesHelper() {
        throw new AssertionError("No instance for you!");
    }

    private static boolean getTeamListActivityBoolean(Context context, String key) {
        return context.getSharedPreferences(TEAM_LIST_ACTIVITY_PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(key, false);
    }

    private static void setTeamListActivityBoolean(Context context, String key, boolean value) {
        context.getSharedPreferences(TEAM_LIST_ACTIVITY_PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(key, value)
                .apply();
    }

    public static boolean hasShownFabTutorial(Context context) {
        return getTeamListActivityBoolean(context, HAS_SHOWN_FAB_TUTORIAL);
    }

    public static void setHasShownFabTutorial(Context context, boolean value) {
        setTeamListActivityBoolean(context, HAS_SHOWN_FAB_TUTORIAL, value);
    }

    public static boolean hasShownSignInTutorial(Context context) {
        return getTeamListActivityBoolean(context, HAS_SHOWN_SIGN_IN_TUTORIAL);
    }

    public static void setHasShownSignInTutorial(Context context, boolean value) {
        setTeamListActivityBoolean(context, HAS_SHOWN_SIGN_IN_TUTORIAL, value);
    }

    public static boolean shouldShowExportHint(Context context) {
        return context.getSharedPreferences(EXPORT_PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(EXPORT_PREF_NAME, true);
    }

    public static void setShouldShowExportHint(Context context, boolean value) {
        context.getSharedPreferences(EXPORT_PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(EXPORT_PREF_NAME, value)
                .apply();
    }
}
