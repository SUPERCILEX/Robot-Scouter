package com.supercilex.robotscouter.ui.scout.viewholder.template;

import android.view.View;

import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;

public class CounterTemplateViewHolder extends CounterViewHolder implements ScoutTemplateViewHolder {
    public CounterTemplateViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void bind() {
        super.bind();
        mName.setOnFocusChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mName.hasFocus()) {
            updateMetricName(mName.getText().toString());
        }
        super.onClick(v);
    }

    @Override
    public void requestFocus() {
        mName.requestFocus();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) updateMetricName(mName.getText().toString());
    }
}
