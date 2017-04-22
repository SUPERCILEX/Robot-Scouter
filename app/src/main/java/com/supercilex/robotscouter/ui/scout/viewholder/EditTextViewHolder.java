package com.supercilex.robotscouter.ui.scout.viewholder;

import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.supercilex.robotscouter.R;

public class EditTextViewHolder extends ScoutViewHolderBase<String, TextView>
        implements View.OnFocusChangeListener {
    private final EditText mNotes;

    public EditTextViewHolder(View itemView) {
        super(itemView);
        mNotes = (EditText) itemView.findViewById(R.id.notes);
    }

    @Override
    @CallSuper
    public void bind() {
        super.bind();
        mNotes.setText(mMetric.getValue());
        mNotes.setOnFocusChangeListener(this);
    }

    @Override
    @CallSuper
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus && v.getId() == R.id.notes) {
            updateMetricValue(mNotes.getText().toString());
        }
    }
}
