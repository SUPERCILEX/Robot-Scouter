package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;
import android.support.v7.widget.SimpleItemAnimator;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

public class ScoutSpinner extends ScoutMetric<ArrayList<String>> {
    private int mSelectedValue;

    public ScoutSpinner() {
        super();
        // Needed for Firebase
    }

    public ScoutSpinner(String name, ArrayList<String> values, int selectedValue) {
        super(name, values);
        mSelectedValue = selectedValue;
    }

    @Override
    public int getType() {
        return Constants.SPINNER;
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
    public String toString() {
        return super.toString() + "\nSelected value = " + mSelectedValue;
    }
}
