package com.supercilex.robotscouter.data.util;

import android.os.Bundle;

import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.model.metrics.CounterMetric;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;
import com.supercilex.robotscouter.data.model.metrics.StopwatchMetric;
import com.supercilex.robotscouter.util.AnalyticsHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScoutUtils {
    public static final SnapshotParser<ScoutMetric> METRIC_PARSER = new SnapshotParser<ScoutMetric>() {
        @Override
        public ScoutMetric parseSnapshot(DataSnapshot snapshot) {
            Integer value = snapshot.child(Constants.FIREBASE_TYPE).getValue(Integer.class);

            if (value == null) { // This should never happen TODO remove if the crash doesn't show up after a while
                FirebaseCrash.log("Snapshot: " + snapshot.toString());
                FirebaseCrash.log("Ref: " + snapshot.getRef().toString());
                FirebaseCrash.report(new NullPointerException()); // NOPMD

                return new ScoutMetric<Void>("", null, MetricType.HEADER);
            }

            ScoutMetric metric;
            @MetricType int type = value;
            switch (type) {
                case MetricType.CHECKBOX:
                    metric = snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {
                    });
                    break;
                case MetricType.COUNTER:
                    metric = new CounterMetric(
                            snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                            snapshot.child(Constants.FIREBASE_VALUE)
                                    .getValue(new GenericTypeIndicator<Integer>() {
                                    }),
                            snapshot.child(Constants.FIREBASE_UNIT).getValue(String.class));
                    break;
                case MetricType.NOTE:
                    metric = snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {
                    });
                    break;
                case MetricType.SPINNER:
                    Map<String, String> values = new LinkedHashMap<>();
                    Iterable<DataSnapshot> children =
                            snapshot.child(Constants.FIREBASE_VALUE).getChildren();
                    for (DataSnapshot snapshot1 : children) {
                        values.put(snapshot1.getKey(), snapshot1.getValue(String.class));
                    }

                    metric = new SpinnerMetric(
                            snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                            values,
                            snapshot.child(Constants.FIREBASE_SELECTED_VALUE_KEY)
                                    .getValue(String.class));
                    break;
                case MetricType.STOPWATCH:
                    metric = new StopwatchMetric(
                            snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                            snapshot.child(Constants.FIREBASE_VALUE)
                                    .getValue(new GenericTypeIndicator<List<Long>>() {
                                    }));
                    break;
                case MetricType.HEADER:
                    metric = snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Void>>() {
                    });
                    break;
                default:
                    throw new IllegalStateException("Unknown metric type: " + type);
            }

            metric.setRef(snapshot.getRef());
            return metric;
        }
    };

    private static final String SCOUT_KEY = "scout_key";

    private ScoutUtils() {
        throw new AssertionError("No instance for you!");
    }

    public static Bundle getScoutKeyBundle(String key) {
        Bundle args = new Bundle();
        args.putString(SCOUT_KEY, key);
        return args;
    }

    public static String getScoutKey(Bundle bundle) {
        return bundle.getString(SCOUT_KEY);
    }

    public static DatabaseReference getIndicesRef(String teamKey) {
        return Constants.FIREBASE_SCOUT_INDICES.child(teamKey);
    }

    public static String add(Team team) {
        DatabaseReference indexRef = getIndicesRef(team.getKey()).push();
        indexRef.setValue(System.currentTimeMillis());
        DatabaseReference scoutRef = Constants.getScoutMetrics(indexRef.getKey());

        FirebaseTransformer scoutCopier = new FirebaseCopier(scoutRef);
        if (team.getTemplateKey() == null) {
            scoutCopier.setFromQuery(Constants.FIREBASE_DEFAULT_TEMPLATE);
        } else {
            scoutCopier.setFromQuery(Constants.FIREBASE_SCOUT_TEMPLATES.child(team.getTemplateKey()));
        }
        scoutCopier.performTransformation();

        AnalyticsHelper.addScout(team.getNumber());

        return indexRef.getKey();
    }

    public static void delete(String teamKey, String scoutKey) {
        Constants.FIREBASE_SCOUTS.child(scoutKey).removeValue();
        getIndicesRef(teamKey).child(scoutKey).removeValue();
    }

    public static Task<Void> deleteAll(String teamKey) {
        final TaskCompletionSource<Void> deleteTask = new TaskCompletionSource<>();
        getIndicesRef(teamKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot keySnapshot : snapshot.getChildren()) {
                    Constants.FIREBASE_SCOUTS.child(keySnapshot.getKey()).removeValue();
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
