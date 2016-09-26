package com.supercilex.robotscouter.models.scout.viewholders;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.models.scout.metrics.ScoutMetric;

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
        setText(view.getName());
        setValue((String) view.getValue());
        setOnFocusChangeListener(view, ref);
    }

    public void setText(String name) {
        mName.setText(name);
    }

    private void setValue(String value) {
        mNotes.setText(value);
    }

    private void setOnFocusChangeListener(final ScoutMetric scoutEditText,
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
