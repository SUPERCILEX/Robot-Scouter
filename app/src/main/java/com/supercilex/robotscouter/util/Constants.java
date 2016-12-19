package com.supercilex.robotscouter.util;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.database.DatabaseReference;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    // The Blue Alliance API
    public static final String TEAM_NICKNAME = "nickname";
    public static final String TEAM_WEBSITE = "website";

    // Intents/Bundles
    public static final String INTENT_TEAM = "com.supercilex.robotscouter.Team";
    public static final String MANAGER_STATE = "manager_state";
    public static final String ITEM_COUNT = "count";
    public static final String SCOUT_KEY = "scout_key";

    // Scout ids
    public static final int CHECKBOX = 0;
    public static final int COUNTER = 1;
    public static final int SPINNER = 2;
    public static final int EDIT_TEXT = 3;
    public static final int SLIDER = 4;

    /**
     * The list of all supported authentication providers in Firebase Auth UI.
     */
    public static final List<AuthUI.IdpConfig> ALL_PROVIDERS =
            Collections.unmodifiableList(
                    Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()));

    // *** CAUTION--DO NOT TOUCH! ***
    // [START FIREBASE CHILD NAMES]
    public static final DatabaseReference FIREBASE_USERS = BaseHelper.getDatabase().child("users");

    // Team
    public static final DatabaseReference FIREBASE_TEAMS = BaseHelper.getDatabase().child("teams");
    public static final DatabaseReference FIREBASE_TEAM_INDICES =
            BaseHelper.getDatabase().child("team-indices");
    public static final String FIREBASE_TIMESTAMP = "timestamp";
    public static final String FIREBASE_TEMPLATE_KEY = "templateKey";

    // Scout
    public static final DatabaseReference FIREBASE_SCOUTS =
            BaseHelper.getDatabase().child("scouts");
    public static final DatabaseReference FIREBASE_SCOUT_INDICES =
            BaseHelper.getDatabase().child("scout-indices");

    // Scout views
    public static final String FIREBASE_VIEWS = "views";
    public static final String FIREBASE_VALUE = "value";
    public static final String FIREBASE_TYPE = "type";
    public static final String FIREBASE_NAME = "name";
    public static final String FIREBASE_SELECTED_VALUE = "selectedValue";

    // Scout template
    public static final DatabaseReference FIREBASE_DEFAULT_TEMPLATE =
            BaseHelper.getDatabase().child("default-template");
    public static final DatabaseReference FIREBASE_SCOUT_TEMPLATES =
            BaseHelper.getDatabase().child("scout-templates");
    // [END FIREBASE CHILD NAMES]

    private Constants() {
        // no instance
    }
}
