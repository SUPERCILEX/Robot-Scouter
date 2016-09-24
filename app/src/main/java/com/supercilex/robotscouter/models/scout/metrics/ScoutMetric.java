package com.supercilex.robotscouter.models.scout.metrics;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.Constants;

public class ScoutMetric<T> {
    private String mName;
    private T mValue;
    private int mType;

    public ScoutMetric() {
    }

    public ScoutMetric(String name, T value) {
        setName(name);
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public T getValue() {
        return mValue;
    }

    public void setValue(T value) {
        mValue = value;
    }

    public void setValue(DatabaseReference databaseReference, T value) {
        databaseReference.child(Constants.FIREBASE_VALUE).setValue(value);
        mValue = value;
    }

    public int getType() {
        return mType;
    }

    public ScoutMetric<T> setType(int type) {
        mType = type;
        return this;
    }

    public void setType(Integer type) {
        mType = type;
    }
}
