package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

@Keep
public class ScoutMetric<T> {
    private String mName;
    private T mValue;
    private int mType;

    public ScoutMetric() {
        // Needed for Firebase
    }

    public ScoutMetric(String name, T value) {
        mName = name;
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

    public void setValue(Query query, T value) {
        query.getRef().child(Constants.FIREBASE_VALUE).setValue(value);
        mValue = value;
    }

    public int getType() {
        return mType;
    }

    public void setType(Integer type) {
        mType = type;
    }

    public ScoutMetric<T> setType(int type) {
        mType = type;
        return this;
    }
}
