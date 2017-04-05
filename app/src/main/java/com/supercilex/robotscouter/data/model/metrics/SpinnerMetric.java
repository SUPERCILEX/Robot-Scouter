package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.util.Constants;

import java.util.Collections;
import java.util.Map;

public class SpinnerMetric extends ScoutMetric<Map<String, String>> {
    @Exclude private String mSelectedValueKey;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public SpinnerMetric() { // Needed for Firebase
        super();
    }

    public SpinnerMetric(String name, Map<String, String> values, String selectedValueKey) {
        super(name, values, MetricType.SPINNER);
        mSelectedValueKey = selectedValueKey;
    }

    public static SpinnerMetric init() {
        return new SpinnerMetric("", Collections.singletonMap("0", "item 1"), null);
    }

    @Keep
    public String getSelectedValueKey() {
        return mSelectedValueKey;
    }

    @Keep
    public void setSelectedValueKey(String key) {
        mSelectedValueKey = key;
    }

    public void updateSelectedValueKey(String key) {
        setSelectedValueKey(key);
        mRef.child(Constants.FIREBASE_SELECTED_VALUE_KEY).setValue(mSelectedValueKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpinnerMetric spinner = (SpinnerMetric) o;

        return TextUtils.equals(mSelectedValueKey, spinner.mSelectedValueKey);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mSelectedValueKey == null ? 0 : mSelectedValueKey.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nSelected value key = " + mSelectedValueKey;
    }
}
