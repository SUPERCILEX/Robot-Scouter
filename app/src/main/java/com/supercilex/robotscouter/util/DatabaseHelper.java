package com.supercilex.robotscouter.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseArray;
import com.firebase.ui.database.FirebaseIndexArray;
import com.firebase.ui.database.ObservableSnapshotArray;
import com.firebase.ui.database.SnapshotParser;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.supercilex.robotscouter.data.client.UploadTeamMediaJobKt.startUploadTeamMediaJob;
import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.updateAnalyticsUserId;
import static com.supercilex.robotscouter.util.ConnectivityUtilsKt.isOffline;
import static com.supercilex.robotscouter.util.Constants.sFirebaseScoutTemplates;
import static com.supercilex.robotscouter.util.Constants.sFirebaseTeams;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_DEFAULT_TEMPLATE;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_METRICS;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_NAME;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_SCOUT_TEMPLATES;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_TEAMS;

public enum DatabaseHelper {;
    private static final String QUERY_KEY = "query_key";

    private static final SnapshotParser<Team> TEAM_PARSER = snapshot -> {
        Team team = snapshot.getValue(Team.class);
        team.setKey(snapshot.getKey());
        return team;
    };
    private static final SnapshotParser<Scout> SCOUT_PARSER = snapshot -> {
        Scout scout = new Scout(snapshot.child(FIREBASE_NAME).getValue(String.class));

        for (DataSnapshot metric : snapshot.child(FIREBASE_METRICS).getChildren()) {
            scout.add(ScoutUtils.METRIC_PARSER.parseSnapshot(metric));
        }

        return scout;
    };

    private static final ObservableSnapshotArray<Team> TEAM_NOOP_ARRAY =
            new NoopArrayBase<>(Team.class);
    private static final ObservableSnapshotArray<Scout> SCOUT_TEMPLATES_NOOP_ARRAY =
            new NoopArrayBase<>(Scout.class);

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

    public static void init(Context appContext) {
        sFirebaseTeams = TEAM_NOOP_ARRAY;
        sFirebaseScoutTemplates = SCOUT_TEMPLATES_NOOP_ARRAY;

        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                removeTeamsListener();
                removeScoutTemplatesListener();
            } else {
                // Log uid to help debug db crashes
                FirebaseCrash.log(user.getUid());
                updateAnalyticsUserId();

                setTeamsListener(appContext);
                setScoutTemplatesListener();
            }
        });

        FIREBASE_DEFAULT_TEMPLATE.addValueEventListener(new ValueEventListener() {
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

    private static void setTeamsListener(final Context appContext) {
        sFirebaseTeams.removeAllListeners();
        sFirebaseTeams = new FirebaseIndexArray<>(
                TeamHelper.getIndicesRef().orderByValue(),
                FIREBASE_TEAMS,
                TEAM_PARSER);

        sFirebaseTeams.addChangeEventListener(new ChangeEventListenerBase() {
            @Override
            public void onChildChanged(EventType type,
                                       DataSnapshot snapshot,
                                       int index,
                                       int oldIndex) {
                if (type == EventType.ADDED || type == EventType.CHANGED) {
                    Team team = sFirebaseTeams.getObject(index);
                    TeamHelper teamHelper = team.getHelper();

                    teamHelper.fetchLatestData(appContext);
                    String media = team.getMedia();
                    if (!TextUtils.isEmpty(media) && new File(media).exists()) {
                        startUploadTeamMediaJob(appContext, teamHelper);
                    }
                }
            }
        });
        sFirebaseTeams.addChangeEventListener(new TeamMergerListener(appContext));
    }

    private static void removeTeamsListener() {
        if (sFirebaseTeams != TEAM_NOOP_ARRAY) {
            sFirebaseTeams.removeAllListeners();
            sFirebaseTeams = TEAM_NOOP_ARRAY;
        }
    }

    private static void setScoutTemplatesListener() {
        sFirebaseScoutTemplates.removeAllListeners();
        sFirebaseScoutTemplates = new FirebaseIndexArray<>(
                UserHelper.getScoutTemplateIndicesRef(),
                FIREBASE_SCOUT_TEMPLATES,
                SCOUT_PARSER);

        sFirebaseScoutTemplates.addChangeEventListener(new ChangeEventListenerBase());
    }

    private static void removeScoutTemplatesListener() {
        if (sFirebaseScoutTemplates != SCOUT_TEMPLATES_NOOP_ARRAY) {
            sFirebaseScoutTemplates.removeAllListeners();
            sFirebaseScoutTemplates = SCOUT_TEMPLATES_NOOP_ARRAY;
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
        private final Context mAppContext;

        public TeamMergerListener(Context appContext) {
            super();
            mAppContext = appContext;
        }

        @Override
        public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
            if (isOffline(mAppContext) || !(type == EventType.ADDED || type == EventType.CHANGED)) {
                return;
            }

            List<TeamHelper> rawTeams = new ArrayList<>();
            for (int j = 0; j < sFirebaseTeams.size(); j++) {
                Team team = sFirebaseTeams.getObject(j);
                TeamHelper rawTeam = new Team.Builder(team)
                        .setTimestamp(0)
                        .setKey(null)
                        .build()
                        .getHelper();

                if (rawTeams.contains(rawTeam)) {
                    List<TeamHelper> teams = Arrays.asList(
                            sFirebaseTeams.getObject(rawTeams.indexOf(rawTeam))
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
            Team oldTeam = teams.remove(0).getTeam();

            for (TeamHelper teamHelper : teams) {
                Team newTeam = teamHelper.getTeam();

                DatabaseHelper.forceUpdate(ScoutUtils.getIndicesRef(newTeam.getKey()))
                        .addOnSuccessListener(query -> new FirebaseCopier(query,
                                                                          ScoutUtils.getIndicesRef(
                                                                                  oldTeam.getKey()))
                                .performTransformation()
                                .continueWithTask(task -> task.getResult().getRef().removeValue())
                                .addOnSuccessListener(aVoid -> newTeam.getHelper().deleteTeam()));
            }
        }
    }
}
