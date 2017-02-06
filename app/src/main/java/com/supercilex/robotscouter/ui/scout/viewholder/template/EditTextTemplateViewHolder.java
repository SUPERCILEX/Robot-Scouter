package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.view.View;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;

public class EditTextTemplateViewHolder extends EditTextViewHolder implements ScoutTemplateViewHolder {
    public EditTextTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind() {
        super.bind();
        mName.setOnFocusChangeListener(this);
    }

    @Override
    public void requestFocus() {
        mName.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        super.onFocusChange(v, hasFocus);
        if (!hasFocus && v.getId() == R.id.name) {
            updateMetricName(mName.getText().toString());
        }
    }
}
