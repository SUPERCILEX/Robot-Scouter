package com.supercilex.robotscouter.data.model;

import android.os.Bundle;
import android.support.annotation.Keep;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.util.FirebaseCopier;
import com.supercilex.robotscouter.data.util.FirebaseTransformer;
import com.supercilex.robotscouter.ui.teamlist.AuthHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class Scout {
    @Exclude private String mOwner;
    @Exclude private Map<String, ScoutMetric> mScoutMetrics = new HashMap<>();

    @Exclude
    public static Bundle getScoutKeyBundle(String key) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.SCOUT_KEY, key);
        return bundle;
    }

    @Exclude
    public static String getScoutKey(Bundle bundle) {
        return bundle.getString(Constants.SCOUT_KEY);
    }

    @Exclude
    public static DatabaseReference getIndicesRef() {
        return Constants.FIREBASE_SCOUT_INDICES.child(AuthHelper.getUid());
    }

    public static String add(Team team) {
        DatabaseReference indexRef = getIndicesRef().push();
        indexRef.setValue(Long.parseLong(team.getNumber()));
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

    public static void delete(String key) {
        getIndicesRef().child(key).removeValue();
        Constants.FIREBASE_SCOUTS.child(key).removeValue();
    }

    public static void deleteAll(String teamNumber) {
        getIndicesRef().orderByValue()
                .equalTo(Long.valueOf(teamNumber))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot keySnapshot : snapshot.getChildren()) {
                            Constants.FIREBASE_SCOUTS.child(keySnapshot.getKey()).removeValue();
                            keySnapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        FirebaseCrash.report(error.toException());
                    }
                });
    }

    @Keep
    public String getOwner() {
        return mOwner;
    }

    @Keep
    public Map<String, ScoutMetric> getViews() {
        return mScoutMetrics;
    }

    public void addView(String key, ScoutMetric view) {
        mScoutMetrics.put(key, view);
    }
}
