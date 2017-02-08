package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.CounterMetric;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;

public class CounterTemplateViewHolder extends CounterViewHolder implements ScoutTemplateViewHolder {
    private EditText mUnit;

    public CounterTemplateViewHolder(View itemView) {
        super(itemView);
        mUnit = (EditText) itemView.findViewById(R.id.unit);
        updateConstraints((ConstraintLayout) itemView);
    }

    @Override
    public void bind() {
        super.bind();
        mCount.setText(String.valueOf(mMetric.getValue()));
        mUnit.setText(((CounterMetric) mMetric).getUnit());

        mName.setOnFocusChangeListener(this);
        mUnit.setOnFocusChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (mName.hasFocus()) updateMetricName(mName.getText().toString());
    }

    @Override
    public void requestFocus() {
        mName.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            updateMetricName(mName.getText().toString());
            ((CounterMetric) mMetric).setUnit(mQuery, mUnit.getText().toString(), mAnimator);
        }
    }

    private void updateConstraints(ConstraintLayout layout) {
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.connect(R.id.count, ConstraintSet.RIGHT, R.id.unit, ConstraintSet.LEFT, 0);
        set.setMargin(R.id.count, ConstraintSet.RIGHT, 0);
        set.setMargin(R.id.count, ConstraintSet.END, 0);
        set.applyTo(layout);
    }
}
