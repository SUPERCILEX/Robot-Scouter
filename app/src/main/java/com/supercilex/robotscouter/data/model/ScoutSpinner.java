package com.supercilex.robotscouter.data.model;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.PropertyName;
import com.supercilex.robotscouter.Constants;

import java.util.ArrayList;

public class ScoutSpinner extends ScoutMetric<ArrayList<String>> {
    private int mSelectedValue;

    public ScoutSpinner() {
    }

    public ScoutSpinner(String name, ArrayList<String> values, int selectedValue) {
        super(name, values);
        mSelectedValue = selectedValue;
    }

    @PropertyName(Constants.FIREBASE_SELECTED_VALUE)
    public int getSelectedValue() {
        return mSelectedValue;
    }

    @PropertyName(Constants.FIREBASE_SELECTED_VALUE)
    public void setSelectedValue(int selectedValue) {
        mSelectedValue = selectedValue;
    }

    public void setSelectedValue(DatabaseReference databaseReference, int selectedValue) {
        databaseReference.child(Constants.FIREBASE_SELECTED_VALUE).setValue(selectedValue);
        mSelectedValue = selectedValue;
    }
}
