package com.supercilex.robotscouter.model.scout.metrics;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.Constants;

import java.util.ArrayList;

public class ScoutSpinner implements ScoutMetric {
    private String mName;
    private ArrayList<String> mItems;
    private int mSelectedValue;

    public ScoutSpinner() {
    }

    public ScoutSpinner(String name, ArrayList<String> values) {
        setName(name);
        mItems = values;
        mSelectedValue = 0;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public ArrayList<String> getValues() {
        return mItems;
    }

    public void setValues(ArrayList<String> values) {
        mItems = values;
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

    @Override
    public int getType() {
        return Constants.SPINNER;
    }
}
