package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.annotation.CallSuper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.ListMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpinnerViewHolder extends ScoutViewHolderBase<Map<String, String>, TextView>
        implements AdapterView.OnItemSelectedListener {
    protected Spinner mSpinner;

    public SpinnerViewHolder(View itemView) {
        super(itemView);
        mSpinner = itemView.findViewById(R.id.spinner);
    }

    @Override
    public void bind() {
        super.bind();
        ListMetric listMetric = (ListMetric) getMetric();
        if (listMetric.getValue().isEmpty()) return;

        ArrayAdapter<String> spinnerArrayAdapter = getAdapter(listMetric);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner.setAdapter(spinnerArrayAdapter);
        mSpinner.setOnItemSelectedListener(this);
        mSpinner.setSelection(indexOfKey(listMetric.getSelectedValueKey()));
    }

    @Override
    @CallSuper
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        ListMetric listMetric = (ListMetric) getMetric();
        if (indexOfKey(listMetric.getSelectedValueKey()) != itemPosition) {
            disableAnimations();
            listMetric.updateSelectedValueKey(getKeys().get(itemPosition));
        }
    }

    protected int indexOfKey(String key) {
        List<String> keys = getKeys();
        for (int i = 0; i < keys.size(); i++) {
            if (TextUtils.equals(key, keys.get(i))) return i;
        }

        return 0;
    }

    private List<String> getKeys() {
        return new ArrayList<>(getMetric().getValue().keySet());
    }

    protected ArrayAdapter<String> getAdapter(ListMetric listMetric) {
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  new ArrayList<>(listMetric.getValue().values()));
    }

    @Override
    public void onNothingSelected(AdapterView<?> view) {
        // Cancel
    }
}
