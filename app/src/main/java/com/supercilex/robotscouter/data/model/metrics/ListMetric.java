package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.google.firebase.database.Exclude;

import java.util.Collections;
import java.util.Map;

import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_SELECTED_VALUE_KEY;

public class ListMetric extends ScoutMetric<Map<String, String>> {
    @Exclude private String mSelectedValueKey;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public ListMetric() { // Needed for Firebase
        super();
    }

    public ListMetric(String name, Map<String, String> values, String selectedValueKey) {
        super(name, values, MetricType.LIST);
        mSelectedValueKey = selectedValueKey;
    }

    public static ListMetric init() {
        return new ListMetric("", Collections.singletonMap("0", "item 1"), null);
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
        mRef.child(FIREBASE_SELECTED_VALUE_KEY).setValue(mSelectedValueKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ListMetric spinner = (ListMetric) o;

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
