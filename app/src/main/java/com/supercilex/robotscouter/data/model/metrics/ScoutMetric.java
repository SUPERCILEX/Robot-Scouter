package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

public class ScoutMetric<T> {
    @Exclude private DatabaseReference mRef;
    @Exclude private String mName;
    @Exclude private T mValue;
    @Exclude
    @MetricType
    private int mType;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public ScoutMetric() {
        // Needed for Firebase
    }

    public ScoutMetric(String name, T value, @MetricType int type) {
        mName = name;
        mValue = value;
        mType = type;
    }

    @Exclude
    public DatabaseReference getRef() {
        return mRef;
    }

    @Exclude
    public void setRef(DatabaseReference ref) {
        mRef = ref;
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
        animator.setSupportsChangeAnimations(false);

        mName = name;
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
        animator.setSupportsChangeAnimations(false);

        mValue = value;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoutMetric<?> metric = (ScoutMetric<?>) o;

        return mType == metric.mType
                && mRef.equals(metric.mRef)
                && TextUtils.equals(mName, metric.mName)
                && (mValue == null ? metric.mValue == null : mValue.equals(metric.mValue));
    }

    @Override
    public int hashCode() {
        int result = mRef.hashCode();
        result = 31 * result + (mName == null ? 0 : mName.hashCode());
        result = 31 * result + (mValue == null ? 0 : mValue.hashCode());
        result = 31 * result + mType;
        return result;
    }

    @Override
    public String toString() {
        String metricType = null;
        if (getType() == MetricType.CHECKBOX) metricType = "Checkbox";
        else if (getType() == MetricType.COUNTER) metricType = "Counter";
        else if (getType() == MetricType.NOTE) metricType = "Note";
        else if (getType() == MetricType.SPINNER) metricType = "Spinner";
        else if (getType() == MetricType.STOPWATCH) metricType = "Spinner";

        return metricType + " (" + mRef.getKey() + ")" + " \"" + mName + "\": " + mValue;
    }
}
