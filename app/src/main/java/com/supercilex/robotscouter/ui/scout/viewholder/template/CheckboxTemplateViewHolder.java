package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;

public class CheckboxTemplateViewHolder extends CheckboxViewHolder implements View.OnFocusChangeListener {
    private EditText mCheckBoxName;

    public CheckboxTemplateViewHolder(View itemView) {
        super(itemView);
        mCheckBoxName = (EditText) itemView.findViewById(R.id.checkbox_name);
    }

    @Override
    public void bind(ScoutMetric<Boolean> metric, Query query, SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        mName.setText(null);
        mCheckBoxName.setText(metric.getName());
        mCheckBoxName.setOnFocusChangeListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(mCheckBoxName.getText().toString());
    }
}
