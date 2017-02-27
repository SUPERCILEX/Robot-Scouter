package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.View;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.scout.viewholder.StopwatchViewHolder;

public class StopwatchTemplateViewHolder extends StopwatchViewHolder implements ScoutTemplateViewHolder {
    public StopwatchTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    protected void bind() {
        super.bind();
        mName.setOnFocusChangeListener(this);

        ConstraintLayout layout = (ConstraintLayout) itemView;
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.connect(R.id.list, ConstraintSet.TOP, R.id.name, ConstraintSet.BOTTOM, 0);
        set.applyTo(layout);
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
