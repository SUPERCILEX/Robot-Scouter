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
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
public final class Scouts implements Builder<Task<Map<TeamHelper, List<List<ScoutMetric>>>>>, OnFailureListener, OnSuccessListener<Pair<TeamHelper, List<String>>> {
    private TaskCompletionSource<Map<TeamHelper, List<List<ScoutMetric>>>> mScoutsTask = new TaskCompletionSource<>();
    private Map<TeamHelper, List<List<ScoutMetric>>> mScouts = new ConcurrentHashMap<>();
    private ArrayList<Task<Void>> mScoutMetricsTasks = new ArrayList<>();

    private List<TeamHelper> mTeamHelpers;

    private Scouts(List<TeamHelper> helpers) {
        mTeamHelpers = helpers;
    }

    public static Task<Map<TeamHelper, List<List<ScoutMetric>>>> getAll(List<TeamHelper> teamHelpers) {
        return new Scouts(teamHelpers).build();
    }

    @Override
    public Task<Map<TeamHelper, List<List<ScoutMetric>>>> build() {
        List<Task<Pair<TeamHelper, List<String>>>> scoutIndicesTasks = new ArrayList<>();
        for (final TeamHelper helper : mTeamHelpers) {
            final TaskCompletionSource<Pair<TeamHelper, List<String>>> scoutIndicesTask = new TaskCompletionSource<>();
            scoutIndicesTasks.add(scoutIndicesTask.getTask());

            ScoutUtils.getIndicesRef(helper.getTeam().getKey())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            List<String> scoutKeys = new ArrayList<>();
                            for (DataSnapshot scoutKeyTemplate : snapshot.getChildren()) {
                                scoutKeys.add(scoutKeyTemplate.getKey());
                            }
                            scoutIndicesTask.setResult(new Pair<>(helper, scoutKeys));
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            scoutIndicesTask.setException(error.toException());
                            FirebaseCrash.report(error.toException());
                        }
                    });
        }


        for (Task<Pair<TeamHelper, List<String>>> scoutKeysTask : scoutIndicesTasks) {
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
    public void onSuccess(final Pair<TeamHelper, List<String>> pair) {
        for (String scoutKey : pair.second) {
            final TaskCompletionSource<Void> scoutMetricsTask = new TaskCompletionSource<>();
            mScoutMetricsTasks.add(scoutMetricsTask.getTask());

            Constants.FIREBASE_SCOUTS
                    .child(scoutKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            List<ScoutMetric> scoutMetrics = new ArrayList<>();
                            for (DataSnapshot scoutMetricSnapshot : snapshot.getChildren()) {
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
