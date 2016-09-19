package com.supercilex.robotscouter.model.scout.metrics;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.Constants;

public class ScoutCheckbox implements ScoutMetric {
    private String mName;
    private boolean mValue;

    public ScoutCheckbox() {
    }

    public ScoutCheckbox(String name, boolean value) {
        setName(name);
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isValue() {
        return mValue;
    }

    public void setValue(boolean value) {
        mValue = value;
    }

    @Override
    public int getType() {
        return Constants.CHECKBOX;
    }

    public void setValue(DatabaseReference databaseReference, boolean value) {
        databaseReference.child(Constants.FIREBASE_VALUE).setValue(value);
        mValue = value;
    }
}
