package com.supercilex.robotscouter.data.model;

import android.os.Bundle;
import android.support.annotation.Keep;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.FirebaseTransformer;
import com.supercilex.robotscouter.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class Scout {
    @Exclude private static final String SCOUT_KEY = "scout_key";

    @Exclude private Map<String, ScoutMetric> mScoutMetrics = new HashMap<>();

    @Exclude
    public static Bundle getScoutKeyBundle(String key) {
        Bundle bundle = new Bundle();
        bundle.putString(SCOUT_KEY, key);
        return bundle;
    }

    @Exclude
    public static String getScoutKey(Bundle bundle) {
        return bundle.getString(SCOUT_KEY);
    }

    @Exclude
    public static DatabaseReference getIndicesRef(String teamKey) {
        return Constants.FIREBASE_SCOUT_INDICES.child(teamKey);
    }

    public static String add(Team team) {
        DatabaseReference indexRef = getIndicesRef(team.getKey()).push();
        indexRef.setValue(team.getNumberAsLong());
        DatabaseReference scoutRef = Constants.FIREBASE_SCOUTS.child(indexRef.getKey());

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
        Constants.FIREBASE_SCOUTS.child(scoutKey).removeValue();
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

    @Keep
    public Map<String, ScoutMetric> getViews() {
        return mScoutMetrics;
    }

    public void addView(String key, ScoutMetric view) {
        mScoutMetrics.put(key, view);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scout scout = (Scout) o;

        return mScoutMetrics.equals(scout.mScoutMetrics);
    }

    @Override
    public int hashCode() {
        return mScoutMetrics.hashCode();
    }

    @Override
    public String toString() {
        return "Scout{" +
                "mScoutMetrics=" + mScoutMetrics +
                '}';
    }
}
