package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.SpinnerMetric;
import com.supercilex.robotscouter.ui.scout.template.SpinnerTemplateDialog;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpinnerTemplateViewHolder extends SpinnerViewHolder implements ScoutTemplateViewHolder {
    public SpinnerTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind() {
        super.bind();
        mName.setOnFocusChangeListener(this);
    }

    @Override
    protected ArrayAdapter<String> getAdapter(SpinnerMetric spinnerMetric) {
        Map<String, String> items = new LinkedHashMap<>();
        items.put(mMetric.getRef().push().getKey(),
                  itemView.getContext().getString(R.string.edit_spinner_items));
        items.putAll(spinnerMetric.getValue());
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  new ArrayList<>(items.values()));
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        if (itemPosition == 0) {
            disableAnimations();

            SpinnerMetric spinnerMetric = (SpinnerMetric) mMetric;
            SpinnerTemplateDialog.show(mManager,
                                       mMetric.getRef().child(Constants.FIREBASE_VALUE),
                                       spinnerMetric.getSelectedValueKey());
            mSpinner.setSelection(indexOfKey(spinnerMetric.getSelectedValueKey()));
        } else {
            super.onItemSelected(parent, view, itemPosition - 1, id);
        }
    }

    @Override
    protected int indexOfKey(String key) {
        return super.indexOfKey(key) + 1;
    }

    @Override
    public void requestFocus() {
        mName.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(mName.getText().toString());
    }
}
