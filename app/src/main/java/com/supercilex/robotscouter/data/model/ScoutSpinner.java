package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.PropertyName;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;

public class ScoutSpinner extends ScoutMetric<ArrayList<String>> {
    private int mSelectedValue;

    public ScoutSpinner() {
    }

    public ScoutSpinner(String name, ArrayList<String> values, int selectedValue) {
        super(name, values);
        mSelectedValue = selectedValue;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_SELECTED_VALUE)
    public int getSelectedValue() {
        return mSelectedValue;
    }

    @Keep
    @PropertyName(Constants.FIREBASE_SELECTED_VALUE)
    public void setSelectedValue(int selectedValue) {
        mSelectedValue = selectedValue;
    }

    public void setSelectedValue(DatabaseReference databaseReference, int selectedValue) {
        databaseReference.child(Constants.FIREBASE_SELECTED_VALUE).setValue(selectedValue);
        mSelectedValue = selectedValue;
    }
}
