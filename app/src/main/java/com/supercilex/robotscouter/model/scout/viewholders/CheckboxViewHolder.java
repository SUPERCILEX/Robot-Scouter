package com.supercilex.robotscouter.model.scout.viewholders;

import android.view.View;
import android.widget.CheckBox;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.model.scout.metrics.ScoutCheckbox;
import com.supercilex.robotscouter.model.scout.metrics.ScoutMetric;

public class CheckboxViewHolder extends ScoutViewHolder {
    private CheckBox mName;

    public CheckboxViewHolder(View itemView) {
        super(itemView);
        mName = (CheckBox) itemView.findViewById(R.id.checkbox);
    }

    @Override
    public void initialize(ScoutMetric view, DatabaseReference ref) {
        ScoutCheckbox scoutCheckbox = (ScoutCheckbox) view;

        setText(scoutCheckbox.getName());
        setValue(scoutCheckbox.isValue());
        setOnClickListener(scoutCheckbox, ref);
    }

    public void setText(String name) {
        mName.setText(name);
    }

    private void setValue(boolean value) {
        mName.setChecked(value);
    }

    private boolean isChecked() {
        return mName.isChecked();
    }

    private void setOnClickListener(final ScoutCheckbox scoutCheckbox,
                                    final DatabaseReference ref) {
        mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scoutCheckbox.setValue(ref, isChecked());
            }
        });
    }
}
