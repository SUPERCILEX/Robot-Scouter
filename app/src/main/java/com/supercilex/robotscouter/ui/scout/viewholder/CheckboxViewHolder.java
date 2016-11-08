package com.supercilex.robotscouter.ui.scout.viewholder;

import android.view.View;
import android.widget.CheckBox;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class CheckboxViewHolder extends ScoutViewHolder {
    private CheckBox mName;

    public CheckboxViewHolder(View itemView) {
        super(itemView);
        mName = (CheckBox) itemView.findViewById(R.id.checkbox);
    }

    @Override
    public void initialize(ScoutMetric view, DatabaseReference ref) {
        setText(view.getName());
        setValue((Boolean) view.getValue());
        setOnClickListener(view, ref);
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

    private void setOnClickListener(final ScoutMetric scoutCheckbox,
                                    final DatabaseReference ref) {
        mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scoutCheckbox.setValue(ref, isChecked());
            }
        });
    }
}
