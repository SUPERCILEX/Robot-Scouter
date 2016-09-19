package com.supercilex.robotscouter.model.scout.viewholders;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.model.scout.metrics.ScoutEditText;
import com.supercilex.robotscouter.model.scout.metrics.ScoutMetric;

public class EditTextViewHolder extends ScoutViewHolder {
    private TextView mName;
    private EditText mNotes;

    public EditTextViewHolder(View itemView) {
        super(itemView);
        mName = (TextView) itemView.findViewById(R.id.current_scout_text_view);
        mNotes = (EditText) itemView.findViewById(R.id.current_scout_edit_text);
    }

    @Override
    public void initialize(ScoutMetric view, DatabaseReference ref) {
        ScoutEditText scoutEditText = (ScoutEditText) view;

        setText(scoutEditText.getName());
        setValue(scoutEditText.getValue());
        setOnFocusChangeListener(scoutEditText, ref);
    }

    public void setText(String name) {
        mName.setText(name);
    }

    private void setValue(String value) {
        mNotes.setText(value);
    }

    private void setOnFocusChangeListener(final ScoutEditText scoutEditText,
                                          final DatabaseReference ref) {
        mNotes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    scoutEditText.setValue(ref, mNotes.getText().toString());
                }
            }
        });
    }
}
