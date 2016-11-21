package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.model.ScoutSpinner;

import java.util.ArrayList;

public class SpinnerViewHolder extends ScoutViewHolder<ArrayList<String>, TextView> {
    private Spinner mSpinner;

    public SpinnerViewHolder(View itemView) {
        super(itemView);
        mSpinner = (Spinner) itemView.findViewById(R.id.spinner);
    }

    @Override
    public void bind(ScoutMetric<ArrayList<String>> metric) {
        super.bind(metric);
        ScoutSpinner scoutSpinner = (ScoutSpinner) mMetric;
        ArrayAdapter<String> spinnerArrayAdapter =
                new ArrayAdapter<>(mSpinner.getContext(),
                                   android.R.layout.simple_spinner_item,
                                   scoutSpinner.getValue());
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinnerArrayAdapter);
        mSpinner.setSelection(scoutSpinner.getSelectedValue());
    }

    @Override
    public void setClickListeners(final Query query,
                                  final SimpleItemAnimator animator) {
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView parent,
                                       View view,
                                       int itemPosition,
                                       long id) {
                animator.setSupportsChangeAnimations(false);
                ((ScoutSpinner) mMetric).setSelectedValue(query, itemPosition);
            }

            @Override
            public void onNothingSelected(AdapterView adapterView) {
                // Cancel
            }
        });
    }
}
