package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public class EditTextViewHolder extends ScoutViewHolderBase<String, TextView> implements View.OnFocusChangeListener {
    private EditText mNotes;

    public EditTextViewHolder(View itemView) {
        super(itemView);
        mNotes = (EditText) itemView.findViewById(R.id.notes);
    }

    @Override
    public void bind(ScoutMetric<String> metric, Query query, SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        mNotes.setText(mMetric.getValue());
        mNotes.setOnFocusChangeListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus && v.getId() == R.id.notes) {
            updateMetricValue(mNotes.getText().toString());
        }
    }
}
