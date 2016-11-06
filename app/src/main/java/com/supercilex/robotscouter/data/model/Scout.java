package com.supercilex.robotscouter.data.model;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scout {
    private String mOwner = FirebaseUtils.getUid();
    private Map<String, Object> mScoutMetrics = new LinkedHashMap<>();

    public Scout() {
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public Map<String, Object> getViews() {
        return mScoutMetrics;
    }

    public void setViews(Map<String, Object> views) {
        mScoutMetrics = views;
    }

    private void addView(DatabaseReference database, ScoutMetric view) {
        mScoutMetrics.put(database.push().getKey(), view);
    }

    public void createScoutId(String teamNumber) {
        DatabaseReference index = FirebaseUtils.getDatabase()
                .child(Constants.FIREBASE_SCOUT_INDEXES)
                .child(mOwner)
                .child(teamNumber)
                .push();

        String scoutKey = index.getKey();

        index.setValue(true);

        DatabaseReference scouts = FirebaseUtils.getDatabase()
                .child(Constants.FIREBASE_SCOUTS)
                .child(scoutKey);

        addView(scouts,
                new ScoutMetric<>("example yes or no value pos 1",
                                  false).setType(Constants.CHECKBOX));
        addView(scouts, new ScoutMetric<>("test pos 2", true).setType(Constants.CHECKBOX));
        addView(scouts, new ScoutMetric<>("auto scores pos 3", 0).setType(Constants.COUNTER));
        addView(scouts, new ScoutMetric<>("teleop scores pos 4", 0).setType(Constants.COUNTER));
        ArrayList<String> list = new ArrayList<>();
        list.add("test");
        list.add("test 2");
        list.add("test 3");
        list.add("test 4");
        addView(scouts, new ScoutSpinner("some name", list, 0).setType(Constants.SPINNER));
        addView(scouts, new ScoutSpinner("foobar!", list, 0).setType(Constants.SPINNER));
        addView(scouts,
                new ScoutMetric<>("note 1 pos 5", "some note").setType(Constants.EDIT_TEXT));
        addView(scouts,
                new ScoutMetric<>("note 2 pos 6", "some other note").setType(Constants.EDIT_TEXT));

        scouts.setValue(this);
    }
}
