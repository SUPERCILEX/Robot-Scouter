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
import com.supercilex.robotscouter.data.model.SpinnerMetric;

import java.util.List;

public class SpinnerViewHolder extends ScoutViewHolderBase<List<String>, TextView>
        implements AdapterView.OnItemSelectedListener {
    private Spinner mSpinner;

    public SpinnerViewHolder(View itemView) {
        super(itemView);
        mSpinner = (Spinner) itemView.findViewById(R.id.spinner);
    }

    @Override
    public void bind(ScoutMetric<List<String>> metric, Query query, SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        SpinnerMetric spinnerMetric = (SpinnerMetric) mMetric;
        ArrayAdapter<String> spinnerArrayAdapter =
                new ArrayAdapter<>(mSpinner.getContext(),
                                   android.R.layout.simple_spinner_item,
                                   spinnerMetric.getValue());
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinnerArrayAdapter);
        mSpinner.setSelection(spinnerMetric.getSelectedValueIndex());
        mSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        ((SpinnerMetric) mMetric).setSelectedValue(mQuery, itemPosition, mAnimator);
    }

    @Override
    public void onNothingSelected(AdapterView<?> view) {
        // Cancel
    }
}
