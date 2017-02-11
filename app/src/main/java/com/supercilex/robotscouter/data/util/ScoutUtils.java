package com.supercilex.robotscouter.data.util;

import android.os.Bundle;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.CounterMetric;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.SpinnerMetric;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

public final class ScoutUtils {
    private static final String SCOUT_KEY = "scout_key";

    private ScoutUtils() {
        // no instance
    }

    public static Bundle getScoutKeyBundle(String key) {
        Bundle bundle = new Bundle();
        bundle.putString(SCOUT_KEY, key);
        return bundle;
    }

    public static String getScoutKey(Bundle bundle) {
        return bundle.getString(SCOUT_KEY);
    }

    public static DatabaseReference getIndicesRef(String teamKey) {
        return Constants.FIREBASE_SCOUT_INDICES.child(teamKey);
    }

    public static ScoutMetric getMetric(DataSnapshot snapshot) {
        switch (snapshot.child(Constants.FIREBASE_TYPE).getValue(Integer.class)) {
            case MetricType.CHECKBOX:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {
                });
            case MetricType.COUNTER:
                return new CounterMetric(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        snapshot.child(Constants.FIREBASE_VALUE)
                                .getValue(new GenericTypeIndicator<Integer>() {
                                }),
                        snapshot.child(Constants.FIREBASE_UNIT).getValue(String.class));
            case MetricType.NOTE:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {
                });
            case MetricType.SPINNER:
                return new SpinnerMetric(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        snapshot.child(Constants.FIREBASE_VALUE)
                                .getValue(new GenericTypeIndicator<ArrayList<String>>() {
                                }),
                        snapshot.child(Constants.FIREBASE_SELECTED_VALUE).getValue(Integer.class));
            default:
                throw new IllegalStateException();
        }
    }

    public static String add(Team team) {
        DatabaseReference indexRef = getIndicesRef(team.getKey()).push();
        indexRef.setValue(team.getNumberAsLong());
        DatabaseReference scoutRef = Constants.getScoutMetrics(indexRef.getKey());

        FirebaseTransformer scoutCopier = new FirebaseCopier(scoutRef);
        if (team.getTemplateKey() == null) {
            scoutCopier.setFromQuery(Constants.FIREBASE_DEFAULT_TEMPLATE);
        } else {
            scoutCopier.setFromQuery(Constants.FIREBASE_SCOUT_TEMPLATES.child(team.getTemplateKey()));
        }
        scoutCopier.performTransformation();

        return indexRef.getKey();
    }

    public static void delete(String teamKey, String scoutKey) {
        getIndicesRef(teamKey).child(scoutKey).removeValue();
        Constants.getScoutMetrics(scoutKey).removeValue();
    }

    public static Task<Void> deleteAll(String teamKey) {
        final TaskCompletionSource<Void> deleteTask = new TaskCompletionSource<>();
        getIndicesRef(teamKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot keySnapshot : snapshot.getChildren()) {
                    Constants.getScoutMetrics(keySnapshot.getKey()).removeValue();
                    keySnapshot.getRef().removeValue();
                }
                deleteTask.setResult(null);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                deleteTask.setException(error.toException());
                FirebaseCrash.report(error.toException());
            }
        });
        return deleteTask.getTask();
    }
}
