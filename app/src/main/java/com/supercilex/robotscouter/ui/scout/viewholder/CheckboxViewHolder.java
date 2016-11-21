package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.CheckBox;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class CheckboxViewHolder extends ScoutViewHolder<Boolean, CheckBox> {
    public CheckboxViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind(ScoutMetric<Boolean> metric) {
        super.bind(metric);
        mName.setChecked(mMetric.getValue());
    }

    @Override
    public void setClickListeners(final Query query,
                                  final SimpleItemAnimator animator) {
        mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMetric.setValue(query, mName.isChecked(), animator);
            }
        });
    }
}
