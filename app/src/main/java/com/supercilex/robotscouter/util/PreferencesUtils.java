package com.supercilex.robotscouter.util;

import android.content.Context;
import android.util.Pair;

import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;

public enum PreferencesUtils {;
    private static final String TEAM_LIST_ACTIVITY_PREF_NAME = TeamListActivity.class.getName();
    private static final String EXPORT_PREF_NAME = "spreadsheet_export";
    private static final String UPLOAD_MEDIA_PREF_NAME = "upload_media";


    private static final String HAS_SHOWN_TUTORIAL = "has_shown_tutorial";
    private static final String HAS_SHOWN_ADD_TEAM_TUTORIAL = HAS_SHOWN_TUTORIAL + "_fab";
    private static final String HAS_SHOWN_SIGN_IN_TUTORIAL = HAS_SHOWN_TUTORIAL + "_sign_in";

    private static final String SHOULD_ASK_TO_UPLOAD_MEDIA = "should_ask_to_upload_media";
    private static final String SHOULD_UPLOAD_MEDIA = "should_upload_media_to_tba";

    private static boolean getTeamListActivityBoolean(Context context, String key) {
        return context.getSharedPreferences(TEAM_LIST_ACTIVITY_PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(key, false);
    }

    private static void setTeamListActivityBoolean(Context context, String key, boolean value) {
        context.getSharedPreferences(TEAM_LIST_ACTIVITY_PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(key, value)
                .apply();
    }

    public static boolean hasShownAddTeamTutorial(Context context) {
        return getTeamListActivityBoolean(context, HAS_SHOWN_ADD_TEAM_TUTORIAL);
    }

    public static void setHasShownAddTeamTutorial(Context context, boolean value) {
        setTeamListActivityBoolean(context, HAS_SHOWN_ADD_TEAM_TUTORIAL, value);
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

    /**
     * @return A pair of booleans where the first is whether or not an upload media to TBA
     * confirmation dialog should be shown and the second is whether or not the media should be
     * uploaded to TBA.
     */
    public static Pair<Boolean, Boolean> shouldAskToUploadMediaToTba(Context context) {
        return Pair.create(
                context.getSharedPreferences(UPLOAD_MEDIA_PREF_NAME, Context.MODE_PRIVATE)
                        .getBoolean(SHOULD_ASK_TO_UPLOAD_MEDIA, true),
                context.getSharedPreferences(UPLOAD_MEDIA_PREF_NAME, Context.MODE_PRIVATE)
                        .getBoolean(SHOULD_UPLOAD_MEDIA, false));
    }

    public static void setShouldAskToUploadMediaToTba(Context context,
                                                      Pair<Boolean, Boolean> value) {
        context.getSharedPreferences(UPLOAD_MEDIA_PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(SHOULD_ASK_TO_UPLOAD_MEDIA, value.first)
                .putBoolean(SHOULD_UPLOAD_MEDIA, value.second)
                .apply();
    }
}
