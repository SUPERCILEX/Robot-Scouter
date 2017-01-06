package com.supercilex.robotscouter.ui.scout.template;

import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.ScoutAdapter;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CheckboxTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CounterTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.EditTextTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerTemplateViewHolder;

public class ScoutTemplateAdapter extends ScoutAdapter {
    public ScoutTemplateAdapter(Class<ScoutMetric> modelClass,
                                Class<ScoutViewHolderBase> viewHolderClass,
                                Query query,
                                SimpleItemAnimator animator) {
        super(modelClass, viewHolderClass, query, animator);
    }

    @Override
    public ScoutViewHolderBase onCreateViewHolder(ViewGroup parent, @MetricType int viewType) {
        switch (viewType) {
            case MetricType.CHECKBOX:
                return new CheckboxTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_checkbox,
                                         parent,
                                         false));
            case MetricType.COUNTER:
                return new CounterTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_counter,
                                         parent,
                                         false));
            case MetricType.EDIT_TEXT:
                return new EditTextTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_notes,
                                         parent,
                                         false));
            case MetricType.SPINNER:
                return new SpinnerTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_spinner,
                                         parent,
                                         false));
            default:
                throw new IllegalStateException();
        }
    }
}
