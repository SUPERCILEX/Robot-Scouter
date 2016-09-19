package com.supercilex.robotscouter.model.scout.metrics;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.Constants;

public class ScoutCounter implements ScoutMetric {
    private String mName;
    private int mCount;

    public ScoutCounter() {
    }

    public ScoutCounter(String name) {
        setName(name);
        mCount = 0;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public int getValue() {
        return mCount;
    }

    public void setValue(int value) {
        mCount = value;
    }

    public void setValue(DatabaseReference databaseReference, int value) {
        databaseReference.child(Constants.FIREBASE_VALUE).setValue(value);
        mCount = value;
    }

    @Override
    public int getType() {
        return Constants.COUNTER;
    }
}
