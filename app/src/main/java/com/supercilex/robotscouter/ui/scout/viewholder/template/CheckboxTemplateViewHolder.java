package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;

public class CheckboxTemplateViewHolder extends CheckboxViewHolder implements ScoutTemplateViewHolder {
    private EditText mCheckBoxName;

    public CheckboxTemplateViewHolder(View itemView) {
        super(itemView);
        mCheckBoxName = (EditText) itemView.findViewById(R.id.checkbox_name);
    }

    @Override
    public void bind() {
        super.bind();
        mName.setText(null);
        mCheckBoxName.setText(mMetric.getName());
        mCheckBoxName.setOnFocusChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mCheckBoxName.hasFocus()) updateMetricName(mCheckBoxName.getText().toString());
        super.onClick(v);
    }

    @Override
    public void requestFocus() {
        mCheckBoxName.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(mCheckBoxName.getText().toString());
    }
}
