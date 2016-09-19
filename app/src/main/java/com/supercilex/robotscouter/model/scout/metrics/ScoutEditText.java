package com.supercilex.robotscouter.model.scout.metrics;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.Constants;

public class ScoutEditText implements ScoutMetric {
    private String mName;
    private String mNote;

    public ScoutEditText() {
    }

    public ScoutEditText(String name, String value) {
        setName(name);
        mNote = value;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getValue() {
        return mNote;
    }

    public void setValue(String value) {
        mNote = value;
    }

    public void setValue(DatabaseReference databaseReference, String value) {
        databaseReference.child(Constants.FIREBASE_VALUE).setValue(value);
        mNote = value;
    }

    @Override
    public int getType() {
        return Constants.EDIT_TEXT;
    }
}
