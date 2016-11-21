package com.supercilex.robotscouter.ui.scout.viewholder;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class EditTextViewHolder extends ScoutViewHolder<String, TextView> {
    private EditText mNotes;

    public EditTextViewHolder(View itemView) {
        super(itemView);
        mNotes = (EditText) itemView.findViewById(R.id.notes);
    }

    @Override
    public void bind(ScoutMetric<String> metric, Query query) {
        super.bind(metric, query);
        mNotes.setText(metric.getValue());
    }

    @Override
    public void setClickListeners(final ScoutMetric<String> metric, final Query query) {
        mNotes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    metric.setValue(query, mNotes.getText().toString());
                }
            }
        });
    }
}
