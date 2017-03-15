package com.supercilex.robotscouter.util;

import android.os.Build;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.AuthHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final String DEBUG_INFO =
            "* Robot Scouter version: " + BuildConfig.VERSION_NAME + "\n" +
                    "* Android OS version: " + Build.VERSION.SDK_INT + "\n" +
                    "* User id: " + AuthHelper.getUid();

    public static final String MANAGER_STATE = "manager_state";
    public static final String ITEM_COUNT = "count";
    public static final String SCOUT_TEMPLATE = "com.supercilex.robotscouter.scout_template";
    public static final int SINGLE_ITEM = 1;

    /** The list of all supported authentication providers in Firebase Auth UI. */
    public static final List<AuthUI.IdpConfig> ALL_PROVIDERS =
            Collections.unmodifiableList(
                    Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()));

    // *** CAUTION--DO NOT TOUCH! ***
    // [START FIREBASE CHILD NAMES]
    public static final DatabaseReference FIREBASE_USERS = DatabaseHelper.getRef().child("users");

    // Team
    public static final DatabaseReference FIREBASE_TEAMS_REF =
            DatabaseHelper.getRef().child("teams");
    public static final DatabaseReference FIREBASE_TEAM_INDICES =
            DatabaseHelper.getRef().child("team-indices");
    public static final String FIREBASE_TIMESTAMP = "timestamp";

    // Scout
    public static final DatabaseReference FIREBASE_SCOUTS = DatabaseHelper.getRef().child("scouts");
    public static final DatabaseReference FIREBASE_SCOUT_INDICES =
            DatabaseHelper.getRef().child("scout-indices");
    public static final String FIREBASE_METRICS = "metrics";

    // Scout views
    public static final String FIREBASE_VALUE = "value";
    public static final String FIREBASE_TYPE = "type";
    public static final String FIREBASE_NAME = "name";
    public static final String FIREBASE_UNIT = "unit";
    public static final String FIREBASE_SELECTED_VALUE_INDEX = "selectedValueIndex";

    // Scout template
    public static final DatabaseReference FIREBASE_DEFAULT_TEMPLATE =
            DatabaseHelper.getRef().child("default-template");
    public static final DatabaseReference FIREBASE_SCOUT_TEMPLATES =
            DatabaseHelper.getRef().child("scout-templates");
    public static final String FIREBASE_TEMPLATE_KEY = "templateKey";
    // [END FIREBASE CHILD NAMES]

    public static ObservableSnapshotArray<Team> sFirebaseTeams;

    private Constants() {
        // no instance
    }

    public static DatabaseReference getScoutMetrics(String key) {
        return FIREBASE_SCOUTS.child(key).child(FIREBASE_METRICS);
    }
}
