package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.CounterMetric;

public class CounterViewHolder extends ScoutViewHolderBase<Integer, TextView> implements View.OnClickListener {
    protected TextView mCount;
    private ImageButton mIncrement;
    private ImageButton mDecrement;

    public CounterViewHolder(View itemView) {
        super(itemView);
        mIncrement = (ImageButton) itemView.findViewById(R.id.increment_counter);
        mCount = (TextView) itemView.findViewById(R.id.count);
        mDecrement = (ImageButton) itemView.findViewById(R.id.decrement_counter);
    }

    @Override
    public void bind() {
        super.bind();
        setValue(mMetric.getValue());
        mIncrement.setOnClickListener(this);
        mDecrement.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.increment_counter) {
            int value = Integer.parseInt(mCount.getText().toString()) + 1;
            setValue(value);
            updateMetricValue(value);
        } else if (id == R.id.decrement_counter
                && Integer.parseInt(mCount.getText().toString()) > 0) { // no negative values
            int value = Integer.parseInt(mCount.getText().toString()) - 1;
            setValue(value);
            updateMetricValue(value);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setValue(int value) {
        String unit = ((CounterMetric) mMetric).getUnit();
        if (unit == null) {
            mCount.setText(String.valueOf(value));
        } else {
            mCount.setText(value + unit);
        }
    }
}
