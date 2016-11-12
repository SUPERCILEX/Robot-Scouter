package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.util.Constants;

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

    @Keep
    public String getName() {
        return mName;
    }

    @Keep
    public void setName(String name) {
        mName = name;
    }

    @Keep
    public T getValue() {
        return mValue;
    }

    @Keep
    public void setValue(T value) {
        mValue = value;
    }

    public void setValue(DatabaseReference databaseReference, T value) {
        databaseReference.child(Constants.FIREBASE_VALUE).setValue(value);
        mValue = value;
    }

    @Keep
    public int getType() {
        return mType;
    }

    @Keep
    public void setType(Integer type) {
        mType = type;
    }

    public ScoutMetric<T> setType(int type) {
        mType = type;
        return this;
    }
}
