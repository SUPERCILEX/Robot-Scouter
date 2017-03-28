package com.supercilex.robotscouter.data.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.Pair;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.util.ConnectivityHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class Scouts implements Builder<Task<Map<TeamHelper, List<Scout>>>>, OnFailureListener, OnSuccessListener<Pair<TeamHelper, List<String>>> {
    private TaskCompletionSource<Map<TeamHelper, List<Scout>>> mScoutsTask = new TaskCompletionSource<>();
    private Map<TeamHelper, List<Scout>> mScouts = new ConcurrentHashMap<>();
    private ArrayList<Task<Void>> mScoutMetricsTasks = new ArrayList<>();

    private List<TeamHelper> mTeamHelpers;
    private Context mContext;

    private Scouts(@Size(min = 1) List<TeamHelper> helpers, Context appContext) {
        mTeamHelpers = helpers;
        mContext = appContext;
    }

    public static Task<Map<TeamHelper, List<Scout>>> getAll(@Size(min = 1) List<TeamHelper> teamHelpers,
                                                            Context appContext) {
        return new Scouts(teamHelpers, appContext).build();
    }

    @Override
    public Task<Map<TeamHelper, List<Scout>>> build() {
        List<Task<Pair<TeamHelper, List<String>>>> scoutIndicesTasks = new ArrayList<>();
        for (final TeamHelper helper : mTeamHelpers) {
            final TaskCompletionSource<Pair<TeamHelper, List<String>>> scoutIndicesTask = new TaskCompletionSource<>();
            scoutIndicesTasks.add(scoutIndicesTask.getTask());

            DatabaseHelper.forceUpdate(ScoutUtils.getIndicesRef(helper.getTeam().getKey()))
                    .addOnSuccessListener(new OnSuccessListener<Query>() {
                        @Override
                        public void onSuccess(Query query) {
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    List<String> scoutKeys = new ArrayList<>();
                                    for (DataSnapshot scoutKeyTemplate : snapshot.getChildren()) {
                                        scoutKeys.add(scoutKeyTemplate.getKey());
                                    }
                                    scoutIndicesTask.setResult(Pair.create(helper, scoutKeys));
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    scoutIndicesTask.setException(error.toException());
                                    FirebaseCrash.report(error.toException());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(this);
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

            new ScoutListener(Constants.FIREBASE_SCOUTS.child(scoutKey), pair, scoutMetricsTask);
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        mScoutsTask.setException(e);
    }

    private class ScoutListener implements ChildEventListener, ValueEventListener, OnSuccessListener<Void> {
        private static final int TIMEOUT = 1;

        private Query mQuery;
        private DatabaseReference mMetricsQuery;
        private Pair<TeamHelper, List<String>> mPair;
        private TaskCompletionSource<Void> mScoutMetricsTask;

        private Scout mScout = new Scout();
        private Timer mTimer = new Timer();

        public ScoutListener(Query query,
                             Pair<TeamHelper, List<String>> pair,
                             TaskCompletionSource<Void> scoutMetricsTask) {
            mQuery = query;
            mMetricsQuery = mQuery.getRef().child(Constants.FIREBASE_METRICS);
            mPair = pair;
            mScoutMetricsTask = scoutMetricsTask;

            resetTimeout();
            if (ConnectivityHelper.isOffline(mContext)) {
                addListeners();
            } else {
                Tasks.whenAll(DatabaseHelper.forceUpdate(mQuery)).addOnSuccessListener(this);
            }
        }

        @Override
        public void onSuccess(Void aVoid) {
            addListeners();
        }

        private void addListeners() {
            mMetricsQuery.addChildEventListener(ScoutListener.this);
            mQuery.addValueEventListener(ScoutListener.this);
        }

        @Override
        public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
            mScout.add(ScoutUtils.METRIC_PARSER.parseSnapshot(snapshot));
            resetTimeout();
        }

        private void resetTimeout() {
            if (ConnectivityHelper.isOffline(mContext)) {
                mTimer.cancel();
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, TimeUnit.SECONDS.toMillis(TIMEOUT));
            }
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            // This is the hackiest thing ever! See https://github.com/firebase/FirebaseUI-Android/pull/477#issuecomment-270283875
            // for why this hack is necessary.

            mScout.setName(snapshot.child(Constants.FIREBASE_NAME).getValue(String.class));
            finish();
        }

        private void finish() {
            mTimer.cancel();
            mMetricsQuery.removeEventListener((ChildEventListener) ScoutListener.this);
            mQuery.removeEventListener((ValueEventListener) ScoutListener.this);
            if (!mScout.getMetrics().isEmpty()) {
                List<Scout> scouts = mScouts.get(mPair.first);
                if (scouts == null) {
                    List<Scout> scoutList = new ArrayList<>();
                    scoutList.add(mScout);
                    mScouts.put(mPair.first, scoutList);
                } else {
                    scouts.add(mScout);
                    mScouts.put(mPair.first, scouts);
                }
            }

            mScoutMetricsTask.trySetResult(null);
        }

        @Override
        public void onCancelled(DatabaseError error) {
            mScoutMetricsTask.setException(error.toException());
            FirebaseCrash.report(error.toException());
        }

        @Override
        public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
            // Noop
        }

        @Override
        public void onChildRemoved(DataSnapshot snapshot) {
            // Noop
        }

        @Override
        public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
            // Noop
        }
    }
}
