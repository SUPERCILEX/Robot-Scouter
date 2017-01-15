package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;

public class CounterTemplateViewHolder extends CounterViewHolder implements ScoutTemplateViewHolder {
    public CounterTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind(ScoutMetric<Integer> metric, Query query, SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        mName.setOnFocusChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mName.hasFocus()) {
            updateMetricName(mName.getText().toString());
        }
        super.onClick(v);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(mName.getText().toString());
    }

    @Override
    public void requestFocus() {
        mName.requestFocus();
    }
}
