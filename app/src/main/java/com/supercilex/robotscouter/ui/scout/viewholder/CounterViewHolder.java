package com.supercilex.robotscouter.ui.scout.viewholder;

import android.annotation.SuppressLint;
import android.support.annotation.CallSuper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.CounterMetric;
import com.supercilex.robotscouter.ui.scout.ScoutCounterValueDialog;
import com.supercilex.robotscouter.util.Constants;

public class CounterViewHolder extends ScoutViewHolderBase<Integer, TextView> implements View.OnClickListener, View.OnLongClickListener {
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
        mIncrement.setOnLongClickListener(this);
        mDecrement.setOnClickListener(this);
        mDecrement.setOnLongClickListener(this);
    }

    @Override
    @CallSuper
    public void onClick(View v) {
        int id = v.getId();
        int value = Integer.parseInt(getStringWithoutUnit());
        if (id == R.id.increment_counter) {
            value++;
            updateValue(value);
        } else if (id == R.id.decrement_counter && value > 0) { // no negative values
            value--;
            updateValue(value);
        }
    }

    private void updateValue(int value) {
        setValue(value);
        updateMetricValue(value);
    }

    @Override
    public boolean onLongClick(View v) {
        ScoutCounterValueDialog.show(mManager,
                                     mMetric.getRef().child(Constants.FIREBASE_VALUE),
                                     getStringWithoutUnit());
        return true;
    }

    private String getStringWithoutUnit() {
        String unit = ((CounterMetric) mMetric).getUnit();
        String count = mCount.getText().toString();
        return TextUtils.isEmpty(unit) ? count : count.replace(unit, "");
    }

    @SuppressLint("SetTextI18n")
    private void setValue(int value) {
        String unit = ((CounterMetric) mMetric).getUnit();
        mCount.setText(TextUtils.isEmpty(unit) ? String.valueOf(value) : value + unit);
    }
}
