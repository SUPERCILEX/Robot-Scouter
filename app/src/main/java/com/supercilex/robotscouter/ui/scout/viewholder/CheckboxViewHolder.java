package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.CheckBox;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class CheckboxViewHolder extends ScoutViewHolderBase<Boolean, CheckBox> implements View.OnClickListener {
    public CheckboxViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind(ScoutMetric<Boolean> metric, Query query, SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        mName.setChecked(mMetric.getValue());
        mName.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        updateMetricValue(mName.isChecked());
    }
}
