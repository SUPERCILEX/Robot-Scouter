package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.CounterMetric;
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
            if (v.getId() == R.id.name) {
                updateMetricName(mName.getText().toString());
            } else if (v.getId() == R.id.unit) {
                CounterMetric counterMetric = (CounterMetric) mMetric;
                String newUnit = mUnit.getText().toString();

                if (TextUtils.isEmpty(newUnit)) newUnit = null;

                if (!TextUtils.equals(counterMetric.getUnit(), newUnit)) {
                    disableAnimations();
                    counterMetric.updateUnit(newUnit);
                }
            }
        }
    }

    private void updateConstraints(ConstraintLayout layout) {
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.connect(R.id.count, ConstraintSet.RIGHT, R.id.unit, ConstraintSet.LEFT, 0);
        set.setMargin(R.id.count, ConstraintSet.END, 0);

        int[] chainIds = {R.id.decrement_counter, R.id.count, R.id.unit, R.id.increment_counter};
        set.createHorizontalChain(ConstraintSet.PARENT_ID,
                                  ConstraintSet.LEFT,
                                  ConstraintSet.PARENT_ID,
                                  ConstraintSet.RIGHT,
                                  chainIds,
                                  null,
                                  ConstraintSet.CHAIN_PACKED);
        for (int id : chainIds) set.setHorizontalBias(id, 1F);

        set.applyTo(layout);
    }
}
