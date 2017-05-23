package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.ListMetric;
import com.supercilex.robotscouter.ui.scout.template.SpinnerTemplateDialog;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_VALUE;

public class SpinnerTemplateViewHolder extends SpinnerViewHolder implements ScoutTemplateViewHolder {
    public SpinnerTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind() {
        super.bind();
        getName().setOnFocusChangeListener(this);
    }

    @Override
    protected ArrayAdapter<String> getAdapter(ListMetric listMetric) {
        Map<String, String> items = new LinkedHashMap<>();
        items.put(getMetric().getRef().push().getKey(),
                  itemView.getContext().getString(R.string.edit_spinner_items));
        items.putAll(listMetric.getValue());
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  new ArrayList<>(items.values()));
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        if (itemPosition == 0) {
            disableAnimations();
            updateMetricName(getName().getText().toString());

            ListMetric listMetric = (ListMetric) getMetric();
            SpinnerTemplateDialog.Companion.show(getManager(),
                                                 getMetric().getRef().child(FIREBASE_VALUE),
                                                 listMetric.getSelectedValueKey());
            mSpinner.setSelection(indexOfKey(listMetric.getSelectedValueKey()));
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
        getName().requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(getName().getText().toString());
    }
}
