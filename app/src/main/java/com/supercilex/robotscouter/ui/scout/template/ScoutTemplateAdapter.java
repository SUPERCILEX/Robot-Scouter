package com.supercilex.robotscouter.ui.scout.template;

import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.ScoutAdapter;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CheckboxTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CounterTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.EditTextTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerTemplateViewHolder;

public class ScoutTemplateAdapter extends ScoutAdapter {
    private ScoutTemplateItemTouchCallback<ScoutMetric, ScoutViewHolderBase> mCallback;

    public ScoutTemplateAdapter(Class<ScoutMetric> modelClass,
                                Class<ScoutViewHolderBase> viewHolderClass,
                                Query query,
                                FragmentManager manager,
                                SimpleItemAnimator animator,
                                ScoutTemplateItemTouchCallback<ScoutMetric, ScoutViewHolderBase> touchCallback) {
        super(modelClass, viewHolderClass, query, manager, animator);
        mCallback = touchCallback;
    }

    @Override
    public void populateViewHolder(ScoutViewHolderBase viewHolder,
                                   ScoutMetric metric,
                                   int position) {
        super.populateViewHolder(viewHolder, metric, position);
        mCallback.updateDragStatus(viewHolder, position);
    }

    @Override
    public ScoutViewHolderBase onCreateViewHolder(ViewGroup parent, @MetricType int viewType) {
        switch (viewType) {
            case MetricType.CHECKBOX:
                return new CheckboxTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_checkbox, parent, false));
            case MetricType.COUNTER:
                return new CounterTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_counter, parent, false));
            case MetricType.NOTE:
                return new EditTextTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_notes, parent, false));
            case MetricType.SPINNER:
                return new SpinnerTemplateViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_template_spinner, parent, false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
        if (mCallback.onChildChanged(type, index)) {
            super.onChildChanged(type, index, oldIndex);
        }
    }
}
