package com.supercilex.robotscouter.util;

import android.content.Context;
import android.os.Build;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.AuthHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Constants {;
    public static final String MANAGER_STATE = "manager_state";
    public static final String ITEM_COUNT = "count";
    public static final int SINGLE_ITEM = 1;
    public static final int TWO_ITEMS = 2;

    /** The list of all supported authentication providers in Firebase Auth UI. */
    public static final List<AuthUI.IdpConfig> ALL_PROVIDERS =
            Collections.unmodifiableList(
                    Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()));

    // *** CAUTION--DO NOT TOUCH! ***
    // [START FIREBASE CHILD NAMES]
    public static final DatabaseReference FIREBASE_USERS = DatabaseHelper.getRef().child("users");

    // Team
    public static final DatabaseReference FIREBASE_TEAMS = DatabaseHelper.getRef().child("teams");
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
    public static final String FIREBASE_SELECTED_VALUE_KEY = "selectedValueKey";

    // Scout template
    public static final DatabaseReference FIREBASE_DEFAULT_TEMPLATE =
            DatabaseHelper.getRef().child("default-template");
    public static final DatabaseReference FIREBASE_SCOUT_TEMPLATES =
            DatabaseHelper.getRef().child("scout-templates");
    public static final String FIREBASE_TEMPLATE_KEY = "templateKey";
    public static final String SCOUT_TEMPLATE_INDICES = "scoutTemplateIndices";
    // [END FIREBASE CHILD NAMES]

    public static ObservableSnapshotArray<Team> sFirebaseTeams;

    public static DataSnapshot sDefaultTemplate;
    public static ObservableSnapshotArray<Scout> sFirebaseScoutTemplates;

    public static String sProviderAuthority;

    public static void init(Context context) {
        sProviderAuthority = context.getPackageName() + ".provider";
    }

    public static DatabaseReference getScoutMetrics(String key) {
        return FIREBASE_SCOUTS.child(key).child(FIREBASE_METRICS);
    }

    public static String getDebugInfo() {
        List<String> templateKeys = new ArrayList<>();
        for (DataSnapshot template : sFirebaseScoutTemplates) {
            templateKeys.add(template.getKey());
        }

        return "* Robot Scouter version: " + BuildConfig.VERSION_NAME + "\n" +
                "* Android OS version: " + Build.VERSION.SDK_INT + "\n" +
                "* User id: " + AuthHelper.getUid() + "\n" +
                "* Scout template keys: " + templateKeys;
    }
}
