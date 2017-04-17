package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.util.Constants;

public class ScoutMetric<T> {
    @Exclude protected DatabaseReference mRef;

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
    public String getKey() {
        return mRef.getKey();
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

    public void updateName(String name) {
        setName(name);
        mRef.child(Constants.FIREBASE_NAME).setValue(mName);
    }

    @Keep
    public T getValue() {
        return mValue;
    }

    @Keep
    public void setValue(T value) {
        mValue = value;
    }

    public void updateValue(T value) {
        setValue(value);
        mRef.child(Constants.FIREBASE_VALUE).setValue(mValue);
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
                && (mRef == null ? metric.mRef == null : mRef.equals(metric.mRef))
                && TextUtils.equals(mName, metric.mName)
                && (getValue() == null ? metric.getValue() == null : getValue().equals(metric.getValue()));
    }

    @Override
    public int hashCode() {
        int result = mRef == null ? 0 : mRef.hashCode();
        result = 31 * result + (mName == null ? 0 : mName.hashCode());
        result = 31 * result + (getValue() == null ? 0 : getValue().hashCode());
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
        else if (getType() == MetricType.STOPWATCH) metricType = "Stopwatch";
        else if (getType() == MetricType.HEADER) metricType = "Header";

        String key = mRef == null ? null : mRef.getKey();
        return metricType + " (" + key + ")" + " \"" + mName + "\": " + getValue();
    }
}
