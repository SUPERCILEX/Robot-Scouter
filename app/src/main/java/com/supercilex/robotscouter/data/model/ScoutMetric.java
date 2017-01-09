package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;
import android.support.v7.widget.SimpleItemAnimator;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

public class ScoutMetric<T> {
    @Exclude private String mName;
    @Exclude private T mValue;
    @Exclude
    @MetricType
    private int mType;

    public ScoutMetric() {
        // Needed for Firebase
    }

    public ScoutMetric(String name, T value) {
        mName = name;
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
    @MetricType
    public int getType() {
        return mType;
    }

    @Keep
    public void setType(@MetricType int type) {
        mType = type;
    }


    @SuppressWarnings("PMD.UselessParentheses")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoutMetric<?> metric = (ScoutMetric<?>) o;

        return mType == metric.mType
                && (mName == null ? metric.mName == null : mName.equals(metric.mName))
                && (mValue == null ? metric.mValue == null : mValue.equals(metric.mValue));
    }

    @Override
    public int hashCode() {
        int result = mName == null ? 0 : mName.hashCode();
        result = 31 * result + (mValue == null ? 0 : mValue.hashCode());
        result = 31 * result + mType;
        return result;
    }

    @Override
    public String toString() {
        String metricType;
        if (getType() == MetricType.CHECKBOX) metricType = "Checkbox";
        else if (getType() == MetricType.COUNTER) metricType = "Counter";
        else if (getType() == MetricType.EDIT_TEXT) metricType = "Note";
        else if (getType() == MetricType.SPINNER) metricType = "Spinner";
        else throw new IllegalStateException();
        return metricType + " \"" + mName + "\": " + mValue;
    }
}
