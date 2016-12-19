package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;
import android.support.v7.widget.SimpleItemAnimator;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

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

    @Exclude
    public static ScoutMetric getMetric(DataSnapshot snapshot) {
        switch (snapshot.child(Constants.FIREBASE_TYPE).getValue(Integer.class)) {
            case Constants.CHECKBOX:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Boolean>>() {
                });
            case Constants.COUNTER:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<Integer>>() {
                });
            case Constants.EDIT_TEXT:
                return snapshot.getValue(new GenericTypeIndicator<ScoutMetric<String>>() {
                });
            case Constants.SPINNER:
                return new ScoutSpinner(
                        snapshot.child(Constants.FIREBASE_NAME).getValue(String.class),
                        snapshot.child(Constants.FIREBASE_VALUE)
                                .getValue(new GenericTypeIndicator<ArrayList<String>>() {
                                }),
                        snapshot.child(Constants.FIREBASE_SELECTED_VALUE).getValue(Integer.class));
            default:
                throw new IllegalStateException();
        }
    }

    @Keep
    public String getName() {
        return mName;
    }

    @Keep
    public void setName(String name) {
        mName = name;
    }

    public void setName(Query query, String name, SimpleItemAnimator animator) {
        mName = name;
        animator.setSupportsChangeAnimations(false);
        query.getRef().child(Constants.FIREBASE_NAME).setValue(mName);
    }

    @Keep
    public T getValue() {
        return mValue;
    }

    @Keep
    public void setValue(T value) {
        mValue = value;
    }

    public void setValue(Query query, T value, SimpleItemAnimator animator) {
        mValue = value;
        animator.setSupportsChangeAnimations(false);
        query.getRef().child(Constants.FIREBASE_VALUE).setValue(mValue);
    }

    @Keep
    public int getType() {
        return mType;
    }

    @Keep
    public void setType(int type) {
        mType = type;
    }

    @Override
    public String toString() {
        String metricType;
        if (mType == Constants.CHECKBOX) metricType = "Checkbox";
        else if (mType == Constants.COUNTER) metricType = "Counter";
        else if (mType == Constants.EDIT_TEXT) metricType = "Note";
        else if (mType == Constants.SPINNER) metricType = "Spinner";
        else throw new IllegalStateException();
        return metricType + " \"" + mName + "\": " + mValue;
    }
}
