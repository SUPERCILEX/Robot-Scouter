package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.annotation.CallSuper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpinnerViewHolder extends ScoutViewHolderBase<Map<String, String>, TextView>
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
        if (spinnerMetric.getValue().isEmpty()) return;

        ArrayAdapter<String> spinnerArrayAdapter = getAdapter(spinnerMetric);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinnerArrayAdapter);
        mSpinner.setOnItemSelectedListener(this);
        mSpinner.setSelection(indexOfKey(spinnerMetric.getSelectedValueKey()));
    }

    @Override
    @CallSuper
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        SpinnerMetric spinnerMetric = (SpinnerMetric) mMetric;
        if (indexOfKey(spinnerMetric.getSelectedValueKey()) != itemPosition) {
            disableAnimations();
            spinnerMetric.updateSelectedValueKey(getKeys().get(itemPosition));
        }
    }

    protected int indexOfKey(String key) {
        List<String> keys = getKeys();
        for (int i = 0; i < keys.size(); i++) {
            if (TextUtils.equals(key, keys.get(i))) return i;
        }

        return 0;
    }

    private ArrayList<String> getKeys() {
        return new ArrayList<>(mMetric.getValue().keySet());
    }

    protected ArrayAdapter<String> getAdapter(SpinnerMetric spinnerMetric) {
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  new ArrayList<>(spinnerMetric.getValue().values()));
    }

    @Override
    public void onNothingSelected(AdapterView<?> view) {
        // Cancel
    }
}
