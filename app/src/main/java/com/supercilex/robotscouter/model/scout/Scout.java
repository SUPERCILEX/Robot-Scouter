package com.supercilex.robotscouter.model.scout;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.Constants;
import com.supercilex.robotscouter.Utils;
import com.supercilex.robotscouter.model.scout.metrics.ScoutCheckbox;
import com.supercilex.robotscouter.model.scout.metrics.ScoutCounter;
import com.supercilex.robotscouter.model.scout.metrics.ScoutEditText;
import com.supercilex.robotscouter.model.scout.metrics.ScoutSpinner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scout {
    private String mOwner = Utils.getUser().getUid();
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

    private void addView(DatabaseReference database, Object view) {
        mScoutMetrics.put(database.push().getKey(), view);
    }

    public void createScoutId(String teamNumber) {
        DatabaseReference index = Utils.getDatabase()
                .getReference()
                .child(Constants.FIREBASE_SCOUT_INDEXES)
                .child(mOwner)
                .child(teamNumber)
                .push();

        String scoutKey = index.getKey();

        index.setValue(true);

        DatabaseReference scouts = Utils.getDatabase()
                .getReference()
                .child(Constants.FIREBASE_SCOUTS)
                .child(scoutKey);

        addView(scouts, new ScoutCheckbox("example yes or no value pos 1", false));
        addView(scouts, new ScoutCheckbox("test pos 2", true));
        addView(scouts, new ScoutCounter("auto scores pos 3"));
        addView(scouts, new ScoutCounter("teleop scores pos 4"));
        addView(scouts, new ScoutEditText("note 1 pos 5", "some note"));
        addView(scouts, new ScoutEditText("note 2 pos 6", "some other note"));
        ArrayList<String> list = new ArrayList<>();
        list.add("test");
        list.add("test 2");
        list.add("test 3");
        list.add("test 4");
        addView(scouts, new ScoutSpinner("some name", list));
        addView(scouts, new ScoutSpinner("foobar!", list));

        scouts.setValue(this);
    }
}
