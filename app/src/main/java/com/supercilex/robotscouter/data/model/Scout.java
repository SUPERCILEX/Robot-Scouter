package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scout {
    private String mOwner;
    private Map<String, ScoutMetric> mScoutMetrics;

    @Exclude
    public static DatabaseReference getIndicesRef() {
        return BaseHelper.getDatabase()
                .child(Constants.FIREBASE_SCOUT_INDICES)
                .child(BaseHelper.getUid());
    }

    @Keep
    public String getOwner() {
        return mOwner;
    }

    @Keep
    public void setOwner(String owner) {
        mOwner = owner;
    }

    @Keep
    public Map<String, ScoutMetric> getViews() {
        return mScoutMetrics;
    }

    @Keep
    public void setViews(Map<String, ScoutMetric> views) {
        mScoutMetrics = views;
    }

    private void addView(DatabaseReference database, ScoutMetric view) {
        mScoutMetrics.put(database.push().getKey(), view);
    }

    public String createScoutId(String teamNumber) {
        DatabaseReference index = getIndicesRef().child(teamNumber).push();
        DatabaseReference scouts = BaseHelper.getDatabase()
                .child(Constants.FIREBASE_SCOUTS)
                .child(index.getKey());
        mScoutMetrics = new LinkedHashMap<>();

        addView(scouts, new ScoutMetric<>("example yes or no value pos 1",
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
        index.setValue(true);

        return index.getKey();
    }
}
