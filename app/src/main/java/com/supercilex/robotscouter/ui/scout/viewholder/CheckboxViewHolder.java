package com.supercilex.robotscouter.ui.scout.viewholder;

import android.view.View;
import android.widget.CheckBox;

public class CheckboxViewHolder extends ScoutViewHolderBase<Boolean, CheckBox> implements View.OnClickListener {
    public CheckboxViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind() {
        super.bind();
        mName.setChecked(mMetric.getValue());
        mName.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        updateMetricValue(mName.isChecked());
    }
}
