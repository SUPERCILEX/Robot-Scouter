package com.supercilex.robotscouter.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.FirebaseIndexArray;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.data.util.UserHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DatabaseHelper {
    private static final String QUERY_KEY = "query_key";

    private static final SnapshotParser<Team> TEAM_PARSER = new SnapshotParser<Team>() {
        @Override
        public Team parseSnapshot(DataSnapshot snapshot) {
            Team team = snapshot.getValue(Team.class);
            team.setKey(snapshot.getKey());
            return team;
        }
    };
    private static final SnapshotParser<Scout> SCOUT_PARSER = new SnapshotParser<Scout>() {
        @Override
        public Scout parseSnapshot(DataSnapshot snapshot) {
            Scout scout = new Scout(snapshot.child(Constants.FIREBASE_NAME).getValue(String.class));

            for (DataSnapshot metric : snapshot.child(Constants.FIREBASE_METRICS).getChildren()) {
                scout.add(ScoutUtils.METRIC_PARSER.parseSnapshot(metric));
            }

            return scout;
        }
    };

    private static final ObservableSnapshotArray<Team> TEAM_NOOP_ARRAY =
            new NoopArrayBase<>(Team.class);
    private static final ObservableSnapshotArray<Scout> SCOUT_TEMPLATES_NOOP_ARRAY =
            new NoopArrayBase<>(Scout.class);

    private DatabaseHelper() {
        throw new AssertionError("No instance for you!");
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
        return DatabaseHolder.DATABASE.getReference(bundle.getString(QUERY_KEY));
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
                setResult();
            }

            @Override
            public void onDataChanged() {
                setResult();
            }

            private void setResult() {
                updater.removeChangeEventListener(this);
                updateTask.setResult(query);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                updateTask.setException(error.toException());
            }
        });

        return updateTask.getTask();
    }

    public static void init(final Context appContext) {
        Constants.sFirebaseTeams = TEAM_NOOP_ARRAY;
        Constants.sFirebaseScoutTemplates = SCOUT_TEMPLATES_NOOP_ARRAY;

        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    removeTeamsListener();
                    removeScoutTemplatesListener();
                } else {
                    // Log uid to help debug db crashes
                    FirebaseCrash.log(user.getUid());
                    AnalyticsHelper.updateUserId();

                    addTeamsListener(appContext);
                    addScoutTemplatesListener();
                }
            }
        });

        Constants.FIREBASE_DEFAULT_TEMPLATE.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Constants.sDefaultTemplate = snapshot;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                FirebaseCrash.report(error.toException());
            }
        });
    }

    private static void addTeamsListener(final Context appContext) {
        Constants.sFirebaseTeams.removeAllListeners();
        Constants.sFirebaseTeams = new FirebaseIndexArray<>(
                TeamHelper.getIndicesRef(),
                Constants.FIREBASE_TEAMS,
                TEAM_PARSER);

        Constants.sFirebaseTeams.addChangeEventListener(new ChangeEventListenerBase() {
            @Override
            public void onChildChanged(EventType type,
                                       DataSnapshot snapshot,
                                       int index,
                                       int oldIndex) {
                if (type == EventType.ADDED) {
                    Constants.sFirebaseTeams.getObject(index)
                            .getHelper()
                            .fetchLatestData(appContext);
                }
            }
        });
        Constants.sFirebaseTeams.addChangeEventListener(new TeamMergerListener(appContext));
    }

    private static void removeTeamsListener() {
        if (Constants.sFirebaseTeams != TEAM_NOOP_ARRAY) {
            Constants.sFirebaseTeams.removeAllListeners();
            Constants.sFirebaseTeams = TEAM_NOOP_ARRAY;
        }
    }

    private static void addScoutTemplatesListener() {
        Constants.sFirebaseScoutTemplates.removeAllListeners();
        Constants.sFirebaseScoutTemplates = new FirebaseIndexArray<>(
                UserHelper.getScoutTemplateIndicesRef(),
                Constants.FIREBASE_SCOUT_TEMPLATES,
                SCOUT_PARSER);

        Constants.sFirebaseScoutTemplates.addChangeEventListener(new ChangeEventListenerBase());
    }

    private static void removeScoutTemplatesListener() {
        if (Constants.sFirebaseScoutTemplates != SCOUT_TEMPLATES_NOOP_ARRAY) {
            Constants.sFirebaseScoutTemplates.removeAllListeners();
            Constants.sFirebaseScoutTemplates = SCOUT_TEMPLATES_NOOP_ARRAY;
        }
    }

    public static class ChangeEventListenerBase implements ChangeEventListener {
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
            FirebaseCrash.report(error.toException());
        }
    }

    private static final class DatabaseHolder {
        public static final FirebaseDatabase DATABASE = FirebaseDatabase.getInstance();
        public static final DatabaseReference INSTANCE;

        static {
            DATABASE.setPersistenceEnabled(true);
            INSTANCE = DATABASE.getReference();
        }
    }

    private static final class NoopArrayBase<T> extends ObservableSnapshotArray<T> {
        public NoopArrayBase(@NonNull Class<T> clazz) {
            super(clazz);
        }

        @Override
        protected List<DataSnapshot> getSnapshots() {
            return new ArrayList<>();
        }
    }

    private static final class TeamMergerListener extends ChangeEventListenerBase {
        private Context mAppContext;

        public TeamMergerListener(Context appContext) {
            super();
            mAppContext = appContext;
        }

        @Override
        public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
            if (ConnectivityHelper.isOffline(mAppContext) && !(type == EventType.ADDED || type == EventType.CHANGED)) {
                return;
            }

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

        private void mergeTeams(List<TeamHelper> teams) {
            Collections.sort(teams);
            final Team oldTeam = teams.remove(0).getTeam();

            for (TeamHelper teamHelper : teams) {
                final Team newTeam = teamHelper.getTeam();

                DatabaseHelper.forceUpdate(ScoutUtils.getIndicesRef(newTeam.getKey()))
                        .addOnSuccessListener(new OnSuccessListener<Query>() {
                            @Override
                            public void onSuccess(Query query) {
                                new FirebaseCopier(query,
                                                   ScoutUtils.getIndicesRef(oldTeam.getKey()))
                                        .performTransformation()
                                        .continueWithTask(new Continuation<DataSnapshot, Task<Void>>() {
                                            @Override
                                            public Task<Void> then(@NonNull Task<DataSnapshot> task) throws Exception {
                                                return task.getResult().getRef().removeValue();
                                            }
                                        })
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                newTeam.getHelper().deleteTeam();
                                            }
                                        });
                            }
                        });
            }
        }
    }
}
