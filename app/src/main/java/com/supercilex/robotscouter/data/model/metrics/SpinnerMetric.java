package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;

import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.util.Constants;

import java.util.List;

public class SpinnerMetric extends ScoutMetric<List<String>> {
    @Exclude private int mSelectedValueIndex;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public SpinnerMetric() { // Needed for Firebase
        super();
    }

    public SpinnerMetric(String name, List<String> values, int selectedValueIndex) {
        super(name, values, MetricType.SPINNER);
        mSelectedValueIndex = selectedValueIndex;
    }

    @Keep
    public int getSelectedValueIndex() {
        return mSelectedValueIndex;
    }

    @Keep
    public void setSelectedValueIndex(int i) {
        mSelectedValueIndex = i;
    }

    public void updateSelectedValueIndex(int i) {
        setSelectedValueIndex(i);
        mRef.child(Constants.FIREBASE_SELECTED_VALUE_INDEX).setValue(mSelectedValueIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpinnerMetric spinner = (SpinnerMetric) o;

        return mSelectedValueIndex == spinner.mSelectedValueIndex;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mSelectedValueIndex;
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nSelected value = " + mSelectedValueIndex;
    }
}
