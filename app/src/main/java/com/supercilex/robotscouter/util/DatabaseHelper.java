package com.supercilex.robotscouter.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.FirebaseIndexArray;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.data.util.TeamHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DatabaseHelper {
    public static final SnapshotParser<Team> TEAM_PARSER = new SnapshotParser<Team>() {
        @Override
        public Team parseSnapshot(DataSnapshot snapshot) {
            Team team = snapshot.getValue(Team.class);
            team.setKey(snapshot.getKey());
            return team;
        }
    };

    private static final String QUERY_KEY = "query_key";

    private static final ObservableSnapshotArray<Team> NOOP_ARRAY =
            new ObservableSnapshotArray<Team>(Team.class) {
                @Override
                protected List<DataSnapshot> getSnapshots() {
                    return new ArrayList<>();
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

    public static Task<Query> forceUpdate(final Query query) {
        final TaskCompletionSource<Query> updateTask = new TaskCompletionSource<>();

        final FirebaseArray updater = new FirebaseArray<>(query, Object.class);
        updater.addChangeEventListener(new ChangeEventListener() {
            @Override
            public void onChildChanged(EventType type,
                                       DataSnapshot snapshot,
                                       int i,
                                       int i1) {
                updater.removeChangeEventListener(this);
                updateTask.setResult(query);
            }

            @Override
            public void onDataChanged() {
                // Noop
            }

            @Override
            public void onCancelled(DatabaseError error) {
                updateTask.setException(error.toException());
            }
        });

        return updateTask.getTask();
    }

    public static void init(final Context appContext) {
        Constants.sFirebaseTeams = NOOP_ARRAY;

        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
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
                    AnalyticsHelper.updateUserId();

                    Constants.sFirebaseTeams.removeAllListeners();
                    Constants.sFirebaseTeams = new FirebaseIndexArray<>(
                            TeamHelper.getIndicesRef(),
                            Constants.FIREBASE_TEAMS_REF,
                            TEAM_PARSER);
                    Constants.sFirebaseTeams.addChangeEventListener(
                            new TeamMergerListener(appContext));
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

    private static class TeamMergerListener implements ChangeEventListener, OnSuccessListener<DatabaseReference> {
        private Context mAppContext;

        public TeamMergerListener(Context appContext) {
            mAppContext = appContext;
        }

        @Override
        public void onChildChanged(EventType type,
                                   DataSnapshot snapshot,
                                   int i,
                                   int i1) {
            if (ConnectivityHelper.isOffline(mAppContext)) return;

            List<TeamHelper> rawTeams = new ArrayList<>();
            for (int j = 0; j < Constants.sFirebaseTeams.size(); j++) {
                Team team = Constants.sFirebaseTeams.getObject(j);
                TeamHelper rawTeam = new Team.Builder(team)
                        .setTimestamp(0)
                        .setKey(null)
                        .build()
                        .getHelper();

                if (rawTeams.contains(rawTeam)) {
                    List<TeamHelper> teams = Arrays.asList(
                            Constants.sFirebaseTeams.getObject(rawTeams.indexOf(rawTeam))
                                    .getHelper(),
                            team.getHelper());
                    mergeTeams(new ArrayList<>(teams));
                    break;
                }

                rawTeams.add(rawTeam);
            }
        }

        private void mergeTeams(final List<TeamHelper> teams) {
            Collections.sort(teams);
            final Team oldTeam = teams.remove(0).getTeam();

            for (TeamHelper teamHelper : teams) {
                final Team newTeam = teamHelper.getTeam();

                DatabaseHelper.forceUpdate(ScoutUtils.getIndicesRef(newTeam.getKey()))
                        .addOnSuccessListener(new OnSuccessListener<Query>() {
                            @Override
                            public void onSuccess(Query query) {
                                Task<List<Task<DatabaseReference>>> copyTask =
                                        new FirebaseCopier(query,
                                                           ScoutUtils.getIndicesRef(oldTeam.getKey()))
                                                .performTransformation();
                                copyTask.addOnSuccessListener(new OnSuccessListener<List<Task<DatabaseReference>>>() {
                                    @Override
                                    public void onSuccess(List<Task<DatabaseReference>> tasks) {
                                        for (Task<DatabaseReference> task : tasks) {
                                            task.addOnSuccessListener(TeamMergerListener.this);
                                        }

                                        Tasks.whenAll(tasks)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        newTeam.getHelper().deleteTeam();
                                                    }
                                                });
                                    }
                                });
                            }
                        });
            }
        }

        @Override
        public void onSuccess(DatabaseReference ref) {
            ref.removeValue();
        }

        @Override
        public void onDataChanged() {
            // No-op
        }

        @Override
        public void onCancelled(DatabaseError error) {
            FirebaseCrash.report(error.toException());
        }
    }
}
