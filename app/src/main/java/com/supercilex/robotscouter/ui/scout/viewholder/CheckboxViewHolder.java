package com.supercilex.robotscouter.ui.scout.viewholder;

import android.view.View;
import android.widget.CheckBox;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class CheckboxViewHolder extends ScoutViewHolder<Boolean, CheckBox> {
    public CheckboxViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind(ScoutMetric<Boolean> metric, Query query) {
        super.bind(metric, query);
        mName.setChecked(metric.getValue());
    }

    @Override
    public void setClickListeners(final ScoutMetric<Boolean> metric, final Query query) {
        mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                metric.setValue(query, mName.isChecked());
            }
        });
    }
}
