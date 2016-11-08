package com.supercilex.robotscouter.ui.scout.viewholder;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.ScoutSpinner;

public class SpinnerViewHolder extends ScoutViewHolder {
    private TextView mName;
    private Spinner mSpinner;

    public SpinnerViewHolder(View itemView) {
        super(itemView);
        mName = (TextView) itemView.findViewById(R.id.name);
        mSpinner = (Spinner) itemView.findViewById(R.id.spinner);
    }

    @Override
    public void initialize(ScoutMetric view, DatabaseReference ref) {
        ScoutSpinner scoutSpinner = (ScoutSpinner) view;

        setText(scoutSpinner.getName());
        initializeSpinner(scoutSpinner);
        setOnItemSelectedListener(scoutSpinner, ref);
    }

    public void setText(String name) {
        mName.setText(name);
    }

    private void initializeSpinner(ScoutSpinner scoutSpinner) {
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(mSpinner.getContext(),
                                                                      android.R.layout.simple_spinner_item,
                                                                      scoutSpinner.getValue());
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinnerArrayAdapter);
        mSpinner.setSelection(scoutSpinner.getSelectedValue());
    }

    private void setOnItemSelectedListener(final ScoutSpinner scoutSpinner,
                                           final DatabaseReference ref) {
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int itemPosition,
                                       long id) {
                scoutSpinner.setSelectedValue(ref, itemPosition);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
}
