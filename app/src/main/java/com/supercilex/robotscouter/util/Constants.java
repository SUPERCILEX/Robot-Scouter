package com.supercilex.robotscouter.util;

import com.firebase.ui.auth.AuthUI;
import com.supercilex.robotscouter.BuildConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constants {
    // The Blue Alliance API
    public static final String TEAM_NICKNAME = "nickname";
    public static final String TEAM_WEBSITE = "website";
    public static final String TOKEN = "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME;

    // Intents/Bundles
    public static final String INTENT_TEAM = "com.supercilex.robotscouter.Team";
    public static final String MANAGER_STATE = "manager_state";
    public static final String ITEM_COUNT = "count";
    public static final String SCOUT_KEY = "scout_key";

    // Scout ids
    public static final int CHECKBOX = 0;
    public static final int COUNTER = 1;
    public static final int EDIT_TEXT = 3;
    public static final int SPINNER = 2;
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
    public static final String FIREBASE_TEAM_INDICES = "team-indices";
    public static final String FIREBASE_SCOUT_INDICES = "scout-indices";
    public static final String FIREBASE_TEAMS = "teams";
    public static final String FIREBASE_SCOUTS = "scouts";

    public static final String FIREBASE_NAME = "name";
    public static final String FIREBASE_TIMESTAMP = "timestamp";

    public static final String FIREBASE_VIEWS = "views";
    public static final String FIREBASE_VALUE = "value";
    public static final String FIREBASE_TYPE = "type";
    public static final String FIREBASE_SELECTED_VALUE = "selected-value";

    public static final String FIREBASE_CUSTOM_NAME = "has-custom-name";
    public static final String FIREBASE_CUSTOM_WEBSITE = "has-custom-website";
    public static final String FIREBASE_CUSTOM_MEDIA = "has-custom-media";
    // [END FIREBASE CHILD NAMES]
}
