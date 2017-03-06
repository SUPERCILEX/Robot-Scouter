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
import java.util.List;

public class SpinnerTemplateViewHolder extends SpinnerViewHolder implements ScoutTemplateViewHolder {
    private boolean mIsFakeSelectedValue;

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
        List<String> value = spinnerMetric.getValue();
        List<String> values = value == null ? new ArrayList<String>() : value;
        values.add(0, itemView.getContext().getString(R.string.edit_spinner_items));
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  values);
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        if (itemPosition == 0) {
            if (mIsFakeSelectedValue) return;
            SpinnerTemplateDialog.show(mManager,
                                       mMetric.getRef().child(Constants.FIREBASE_VALUE),
                                       ((SpinnerMetric) mMetric).getSelectedValueIndex());
            mSpinner.setSelection(getSelectedValueIndex());
        } else {
            super.onItemSelected(parent, view, itemPosition - 1, id);
        }
    }

    @Override
    protected int getSelectedValueIndex() {
        int index = super.getSelectedValueIndex() + 1;
        mIsFakeSelectedValue = false;
        if (index >= mSpinner.getAdapter().getCount()) {
            index = 0;
            mIsFakeSelectedValue = true;
        }
        return index;
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
