package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.SpinnerMetric;
import com.supercilex.robotscouter.ui.scout.template.SpinnerTemplateDialog;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;
import com.supercilex.robotscouter.util.Constants;

import java.util.List;

public class SpinnerTemplateViewHolder extends SpinnerViewHolder implements ScoutTemplateViewHolder {
    public SpinnerTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind() {
        super.bind();
        mSpinner.setSelection(getSelectedValueIndex());
        mName.setOnFocusChangeListener(this);
    }

    @Override
    protected ArrayAdapter<String> getAdapter(SpinnerMetric spinnerMetric) {
        List<String> values = spinnerMetric.getValue();
        values.add(0, itemView.getContext().getString(R.string.edit_spinner_items));
        return new ArrayAdapter<>(itemView.getContext(),
                                  android.R.layout.simple_spinner_item,
                                  values);
    }

    @Override
    public void onItemSelected(AdapterView parent, View view, int itemPosition, long id) {
        if (itemPosition == 0) {
            SpinnerTemplateDialog.show(mManager, mQuery.getRef().child(Constants.FIREBASE_VALUE));
            mSpinner.setSelection(getSelectedValueIndex());
        } else {
            super.onItemSelected(parent, view, itemPosition - 1, id);
        }
    }

    private int getSelectedValueIndex() {
        return ((SpinnerMetric) mMetric).getSelectedValueIndex() + 1;
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
