package com.supercilex.robotscouter.data.util;

import android.support.annotation.NonNull;
import android.util.Pair;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
public final class Scouts implements Builder<Task<Map<Team, List<List<ScoutMetric>>>>>, OnFailureListener, OnSuccessListener<Pair<Team, List<String>>> {
    private TaskCompletionSource<Map<Team, List<List<ScoutMetric>>>> mScoutsTask = new TaskCompletionSource<>();
    private Map<Team, List<List<ScoutMetric>>> mScouts = new ConcurrentHashMap<>();
    private ArrayList<Task<Void>> mScoutMetricsTasks = new ArrayList<>();

    private List<Team> mTeams;

    private Scouts(List<Team> teams) {
        mTeams = teams;
    }

    public static Task<Map<Team, List<List<ScoutMetric>>>> getAll(List<Team> teams) {
        return new Scouts(teams).build();
    }

    @Override
    public Task<Map<Team, List<List<ScoutMetric>>>> build() {
        List<Task<Pair<Team, List<String>>>> scoutIndicesTasks = new ArrayList<>();
        for (final Team team : mTeams) {
            final TaskCompletionSource<Pair<Team, List<String>>> scoutIndicesTask = new TaskCompletionSource<>();
            scoutIndicesTasks.add(scoutIndicesTask.getTask());

            ScoutUtils.getIndicesRef(team.getKey())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            List<String> scoutKeys = new ArrayList<>();
                            for (DataSnapshot scoutKeyTemplate : snapshot.getChildren()) {
                                scoutKeys.add(scoutKeyTemplate.getKey());
                            }
                            scoutIndicesTask.setResult(new Pair<>(team, scoutKeys));
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            scoutIndicesTask.setException(error.toException());
                            FirebaseCrash.report(error.toException());
                        }
                    });
        }


        for (Task<Pair<Team, List<String>>> scoutKeysTask : scoutIndicesTasks) {
            scoutKeysTask.addOnSuccessListener(this).addOnFailureListener(this);
        }

        Tasks.whenAll(scoutIndicesTasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Tasks.whenAll(mScoutMetricsTasks)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mScoutsTask.setResult(mScouts);
                            }
                        })
                        .addOnFailureListener(Scouts.this);
            }
        }).addOnFailureListener(this);


        return mScoutsTask.getTask();
    }

    @Override
    public void onSuccess(final Pair<Team, List<String>> pair) {
        for (String scoutKey : pair.second) {
            final TaskCompletionSource<Void> scoutMetricsTask = new TaskCompletionSource<>();
            mScoutMetricsTasks.add(scoutMetricsTask.getTask());

            Constants.FIREBASE_SCOUTS
                    .child(scoutKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            List<ScoutMetric> scoutMetrics = new ArrayList<>();
                            for (DataSnapshot scoutMetricSnapshot :
                                    snapshot.child(Constants.FIREBASE_VIEWS).getChildren()) {
                                scoutMetrics.add(ScoutUtils.getMetric(scoutMetricSnapshot));
                            }

                            List<List<ScoutMetric>> scouts = mScouts.get(pair.first);
                            if (scouts == null) {
                                ArrayList<List<ScoutMetric>> scoutList = new ArrayList<>();
                                scoutList.add(scoutMetrics);
                                mScouts.put(pair.first, scoutList);
                            } else {
                                scouts.add(scoutMetrics);
                                mScouts.put(pair.first, scouts);
                            }

                            scoutMetricsTask.setResult(null);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            scoutMetricsTask.setException(error.toException());
                            FirebaseCrash.report(error.toException());
                        }
                    });
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        mScoutsTask.setException(e);
    }
}
