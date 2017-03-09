package com.supercilex.robotscouter.util;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseIndexArray;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;

import java.util.ArrayList;
import java.util.List;

public final class DatabaseHelper {
    private static final String QUERY_KEY = "query_key";

    private static final ObservableSnapshotArray<Team> NOOP_ARRAY =
            new ObservableSnapshotArray<Team>(Team.class) {
                @Override
                protected List<DataSnapshot> getSnapshots() {
                    return new ArrayList<>();
                }
            };
    private static final ChangeEventListener NOOP_LISTENER = new ChangeEventListener() {
        @Override
        public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
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

    private DatabaseHelper() { // no instance
    }

    public static FirebaseDatabase getDatabase() {
        return DatabaseHolder.DATABASE;
    }

    public static DatabaseReference getRef() {
        return DatabaseHolder.INSTANCE;
    }

    public static Bundle getRefBundle(DatabaseReference ref) {
        Bundle args = new Bundle();
        args.putString(QUERY_KEY, ref.toString().split("firebaseio.com/")[1]);
        return args;
    }

    public static DatabaseReference getRef(Bundle bundle) {
        return DatabaseHelper.getDatabase().getReference(bundle.getString(QUERY_KEY));
    }

    public static void init() {
        Constants.sFirebaseTeams = NOOP_ARRAY;

        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
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
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    if (Constants.sFirebaseTeams != NOOP_ARRAY) {
                        Constants.sFirebaseTeams.removeAllListeners();
                        Constants.sFirebaseTeams = NOOP_ARRAY;
                    }
                } else {
                    // Log uid to help debug db crashes
                    FirebaseCrash.log(user.getUid());

                    Constants.sFirebaseTeams.removeAllListeners();
                    Constants.sFirebaseTeams = new FirebaseIndexArray<>(
                            TeamHelper.getIndicesRef(),
                            Constants.FIREBASE_TEAMS_REF,
                            mTeamParser);
                    Constants.sFirebaseTeams.addChangeEventListener(NOOP_LISTENER);
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
                FirebaseCrash.report(error.toException());
            }
        });
    }

    private static final class DatabaseHolder {
        public static final FirebaseDatabase DATABASE = FirebaseDatabase.getInstance();
        public static final DatabaseReference INSTANCE;

        static {
            DATABASE.setPersistenceEnabled(true);
            INSTANCE = DATABASE.getReference();
        }
    }
}
