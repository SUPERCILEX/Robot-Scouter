package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;

import java.util.List;

public class SpinnerViewHolder extends ScoutViewHolderBase<List<String>, TextView>
        implements AdapterView.OnItemSelectedListener {
    protected Spinner mSpinner;

    public SpinnerViewHolder(View itemView) {
        super(itemView);
        mSpinner = (Spinner) itemView.findViewById(R.id.spinner);
    }

    @Override
    public void bind() {
        super.bind();
        SpinnerMetric spinnerMetric = (SpinnerMetric) mMetric;

        ArrayAdapter<String> spinnerArrayAdapter = getAdapter(spinnerMetric);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinnerArrayAdapter);
        mSpinner.setSelection(spinnerMetric.getSelectedValueIndex());
        mSpinner.setOnItemSelectedListener(this);
    }

    protected ArrayAdapter<String> getAdapter(SpinnerMetric spinnerMetric) {
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  spinnerMetric.getValue());
    }

    @Override
    @CallSuper
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        SpinnerMetric spinnerMetric = (SpinnerMetric) mMetric;
        if (spinnerMetric.getSelectedValueIndex() != itemPosition) {
            spinnerMetric.setSelectedValueIndex(mRef, itemPosition, mAnimator);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> view) {
        // Cancel
    }
}
