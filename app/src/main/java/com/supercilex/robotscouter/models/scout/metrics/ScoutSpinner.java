package com.supercilex.robotscouter.models.scout.metrics;

import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

public class ScoutSpinner extends ScoutMetric<ArrayList<String>> {
    private int mSelectedValue;

    public ScoutSpinner() {
    }

    public ScoutSpinner(String name, ArrayList<String> values, int selectedValue) {
        super(name, values);
        mSelectedValue = selectedValue;
    }

    public int getSelectedValue() {
        return mSelectedValue;
    }

    public void setSelectedValue(int selectedValue) {
        mSelectedValue = selectedValue;
    }

    public void setSelectedValue(DatabaseReference databaseReference, int selectedValue) {
        databaseReference.child("selectedValue").setValue(selectedValue);
        mSelectedValue = selectedValue;
    }
}
