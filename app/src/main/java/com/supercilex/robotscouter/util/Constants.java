package com.supercilex.robotscouter.util;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseIndexArray;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
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

    private static final List<ChangeEventListener> TMP_LISTENERS = new ArrayList<>();
    private static final ObservableSnapshotArray<Team> NOOP_ARRAY =
            new ObservableSnapshotArray<Team>(Team.class) {
                @Override
                public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
                    ChangeEventListener tmpListener = super.addChangeEventListener(listener);
                    TMP_LISTENERS.add(tmpListener);
                    return tmpListener;
                }

                @Override
                public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
                    super.removeChangeEventListener(listener);
                    TMP_LISTENERS.remove(listener);
                }

                @Override
                protected List<DataSnapshot> getSnapshots() {
                    return new ArrayList<>();
                }
            };

    public static ObservableSnapshotArray<Team> sFirebaseTeams = NOOP_ARRAY;

    static {
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            private final ChangeEventListener mNoopListener = new ChangeEventListener() {
                @Override
                public void onChildChanged(EventType type,
                                           DataSnapshot snapshot,
                                           int index,
                                           int oldIndex) {
                    // Noop
                }

                @Override
                public void onDataChanged() {
                    // Noop
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Noop
                }
            };

            private final SnapshotParser<Team> mTeamParser = new SnapshotParser<Team>() {
                @Override
                public Team parseSnapshot(DataSnapshot snapshot) {
                    Team team = snapshot.getValue(Team.class);
                    team.setKey(snapshot.getKey());
                    return team;
                }
            };

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
                if (auth.getCurrentUser() == null) {
                    if (sFirebaseTeams != NOOP_ARRAY) {
                        sFirebaseTeams.removeAllListeners();
                        sFirebaseTeams = NOOP_ARRAY;
                    }
                } else {
                    sFirebaseTeams = new FirebaseIndexArray<>(
                            FIREBASE_TEAM_INDICES.child(auth.getCurrentUser().getUid()),
                            FIREBASE_TEAMS_REF,
                            mTeamParser);
                    sFirebaseTeams.addChangeEventListener(mNoopListener);

                    for (ChangeEventListener listener : TMP_LISTENERS) {
                        sFirebaseTeams.addChangeEventListener(listener);
                    }
                    NOOP_ARRAY.removeAllListeners();
                }
            }
        });

        Constants.FIREBASE_DEFAULT_TEMPLATE.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // There's a bug in Firebase where the cache is not updated with an addListenerForSingleValueEvent
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    private Constants() {
        // no instance
    }

    public static void init() {
        // Needed for java to perform class initialization
    }

    public static DatabaseReference getScoutMetrics(String key) {
        return FIREBASE_SCOUTS.child(key).child(FIREBASE_METRICS);
    }
}
