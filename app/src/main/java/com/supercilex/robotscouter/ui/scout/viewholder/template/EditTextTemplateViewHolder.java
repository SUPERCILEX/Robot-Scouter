package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;

public class EditTextTemplateViewHolder extends EditTextViewHolder implements ScoutTemplateViewHolder {
    public EditTextTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind(ScoutMetric<String> metric, Query query, SimpleItemAnimator animator) {
        super.bind(metric, query, animator);
        mName.setOnFocusChangeListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        super.onFocusChange(v, hasFocus);
        if (!hasFocus && v.getId() == R.id.name) {
            updateMetricName(mName.getText().toString());
        }
    }

    @Override
    public void requestFocus() {
        mName.requestFocus();
    }
}
