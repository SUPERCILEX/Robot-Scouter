package com.supercilex.robotscouter.ui.scout.viewholder;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class CounterViewHolder extends ScoutViewHolder {
    private TextView mName;
    private ImageButton mIncrement;
    private TextView mCount;
    private ImageButton mDecrement;

    public CounterViewHolder(View itemView) {
        super(itemView);
        mName = (TextView) itemView.findViewById(R.id.name);
        mIncrement = (ImageButton) itemView.findViewById(R.id.increment_counter);
        mCount = (TextView) itemView.findViewById(R.id.count);
        mDecrement = (ImageButton) itemView.findViewById(R.id.decrement_counter);
    }

    @Override
    public void initialize(ScoutMetric view, DatabaseReference ref) {
        setText(view.getName());
        setValue((Integer) view.getValue());
        setIncrementListener(view, ref);
        setDecrementListener(view, ref);
    }

    public void setText(String name) {
        mName.setText(name);
    }

    private void setValue(int value) {
        mCount.setText(String.valueOf(value));
    }

    private void setIncrementListener(final ScoutMetric scoutCounter,
                                      final DatabaseReference ref) {
        mIncrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int value = Integer.valueOf(mCount.getText().toString()) + 1;

                mCount.setText(String.valueOf(value));

                scoutCounter.setValue(ref, value);
            }
        });
    }

    private void setDecrementListener(final ScoutMetric scoutCounter,
                                      final DatabaseReference ref) {
        mDecrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Integer.valueOf(mCount.getText().toString()) > 0) {
                    int value = Integer.valueOf(mCount.getText().toString()) - 1;

                    mCount.setText(String.valueOf(value));

                    scoutCounter.setValue(ref, value);
                }
            }
        });
    }
}
