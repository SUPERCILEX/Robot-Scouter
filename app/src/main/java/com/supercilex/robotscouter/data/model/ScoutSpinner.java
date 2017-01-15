package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.SimpleItemAnimator;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

import java.util.List;

public class ScoutSpinner extends ScoutMetric<List<String>> {
    @Exclude private int mSelectedValue;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public ScoutSpinner() {
        super(); // Needed for Firebase
    }

    public ScoutSpinner(String name, List<String> values, int selectedValue) {
        super(name, values, MetricType.SPINNER);
        mSelectedValue = selectedValue;
    }

    @Keep
    public int getSelectedValue() {
        return mSelectedValue;
    }

    @Keep
    public void setSelectedValue(int selectedValue) {
        mSelectedValue = selectedValue;
    }

    public void setSelectedValue(Query query, int selectedValue, SimpleItemAnimator animator) {
        animator.setSupportsChangeAnimations(false);
        query.getRef().child(Constants.FIREBASE_SELECTED_VALUE).setValue(selectedValue);
        mSelectedValue = selectedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ScoutSpinner spinner = (ScoutSpinner) o;

        return mSelectedValue == spinner.mSelectedValue;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mSelectedValue;
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nSelected value = " + mSelectedValue;
    }
}
