package com.supercilex.robotscouter.data.model;

import android.os.Bundle;
import android.support.annotation.Keep;

import com.google.firebase.database.DataSnapshot;
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
    @Exclude private Map<String, ScoutMetric> mScoutMetrics = new HashMap<>(); // NOPMD

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

    public static void add(final Team team) {
        final DatabaseReference indexRef = getIndicesRef().push();
        DatabaseReference scoutRef = Constants.FIREBASE_SCOUTS.child(indexRef.getKey());

        FirebaseTransformer scoutCopier = new FirebaseCopier(scoutRef) {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                super.onDataChange(snapshot);
                indexRef.setValue(Long.parseLong(team.getNumber()));
            }
        };
        if (team.getTemplateKey() == null) {
            scoutCopier.setFromQuery(Constants.FIREBASE_DEFAULT_TEMPLATE);
        } else {
            scoutCopier.setFromQuery(Constants.FIREBASE_SCOUT_TEMPLATES.child(team.getTemplateKey()));
        }
        scoutCopier.performTransformation();
//        mScoutMetrics = new LinkedHashMap<>();
//
//        addView(scoutRef, new ScoutMetric<>("example yes or no value pos 1",
//                                          false).setType((Integer) Constants.CHECKBOX));
//        addView(scoutRef, new ScoutMetric<>("test pos 2", true).setType((Integer) Constants.CHECKBOX));
//        addView(scoutRef, new ScoutMetric<>("auto scores pos 3", 0).setType((Integer) Constants.COUNTER));
//        addView(scoutRef, new ScoutMetric<>("teleop scores pos 4", 0).setType((Integer) Constants.COUNTER));
//        ArrayList<String> list = new ArrayList<>();
//        list.add("test");
//        list.add("test 2");
//        list.add("test 3");
//        list.add("test 4");
//        addView(scoutRef, new ScoutSpinner("some name", list, 0));
//        addView(scoutRef, new ScoutSpinner("foobar!", list, 0));
//        addView(scoutRef,
//                new ScoutMetric<>("note 1 pos 5", "some note").setType((Integer) Constants.EDIT_TEXT));
//        addView(scoutRef,
//                new ScoutMetric<>("note 2 pos 6", "some other note").setType((Integer) Constants.EDIT_TEXT));
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
