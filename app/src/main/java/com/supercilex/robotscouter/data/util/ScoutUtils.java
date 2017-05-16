package com.supercilex.robotscouter.data.util;

import android.os.Bundle;
import android.text.TextUtils;

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
import com.supercilex.robotscouter.data.model.metrics.ListMetric;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.NumberMetric;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.model.metrics.StopwatchMetric;
import com.supercilex.robotscouter.util.AnalyticsUtils;
import com.supercilex.robotscouter.util.Constants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum  ScoutUtils {;
    public static final String SCOUT_KEY = "scout_key";
    public static final SnapshotParser<ScoutMetric> METRIC_PARSER = snapshot -> {
        ScoutMetric metric;
        Integer typeObject = snapshot.child(Constants.FIREBASE_TYPE).getValue(Integer.class);

        if (typeObject == null) {
            // This appears to happen in the in-between state when the metric has been half copied.
            return new ScoutMetric<Void>("Sanity check failed. Please report: bit.ly/RSGitHub.",
                                         null,
                                         MetricType.HEADER);
        }

        @MetricType int type = typeObject;
        switch (type) {
            case MetricType.BOOLEAN:
                metric = snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {});
                break;
            case MetricType.NUMBER:
                metric = new NumberMetric(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        snapshot.child(Constants.FIREBASE_VALUE)
                                .getValue(new GenericTypeIndicator<Integer>() {}),
                        snapshot.child(Constants.FIREBASE_UNIT).getValue(String.class));
                break;
            case MetricType.TEXT:
                metric = snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {});
                break;
            case MetricType.LIST:
                Map<String, String> values = new LinkedHashMap<>();
                Iterable<DataSnapshot> children =
                        snapshot.child(Constants.FIREBASE_VALUE).getChildren();
                for (DataSnapshot snapshot1 : children) {
                    values.put(snapshot1.getKey(), snapshot1.getValue(String.class));
                }

                metric = new ListMetric(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        values,
                        snapshot.child(Constants.FIREBASE_SELECTED_VALUE_KEY)
                                .getValue(String.class));
                break;
            case MetricType.STOPWATCH:
                metric = new StopwatchMetric(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        snapshot.child(Constants.FIREBASE_VALUE)
                                .getValue(new GenericTypeIndicator<List<Long>>() {}));
                break;
            case MetricType.HEADER:
                metric = snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Void>>() {});
                break;
            default:
                throw new IllegalStateException("Unknown metric type: " + type);
        }

        metric.setRef(snapshot.getRef());
        return metric;
    };

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
        AnalyticsUtils.addScout(team.getNumber());

        DatabaseReference indexRef = getIndicesRef(team.getKey()).push();
        indexRef.setValue(System.currentTimeMillis());
        DatabaseReference scoutRef = Constants.getScoutMetrics(indexRef.getKey());

        if (TextUtils.isEmpty(team.getTemplateKey())) {
            FirebaseCopier.copyTo(Constants.sDefaultTemplate, scoutRef);
        } else {
            new FirebaseCopier(Constants.FIREBASE_SCOUT_TEMPLATES.child(team.getTemplateKey()),
                               scoutRef)
                    .performTransformation();
        }

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
