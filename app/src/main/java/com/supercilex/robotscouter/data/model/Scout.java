package com.supercilex.robotscouter.data.model;

import android.os.Bundle;
import android.support.annotation.Keep;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
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

    public static void add(Team team) {
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
