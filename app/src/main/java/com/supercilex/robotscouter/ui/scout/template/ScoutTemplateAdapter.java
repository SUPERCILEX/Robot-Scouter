package com.supercilex.robotscouter.ui.scout.template;

import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.ScoutAdapter;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CheckboxTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CounterTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.EditTextTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.HeaderTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.StopwatchTemplateViewHolder;

public class ScoutTemplateAdapter extends ScoutAdapter {
    private final ScoutTemplateItemTouchCallback<ScoutMetric, ScoutViewHolderBase> mCallback;

    public ScoutTemplateAdapter(Query query,
                                FragmentManager manager,
                                RecyclerView recyclerView,
                                ScoutTemplateItemTouchCallback<ScoutMetric, ScoutViewHolderBase> touchCallback) {
        super(query, manager, recyclerView);
        mCallback = touchCallback;
    }

    @Override
    public void populateViewHolder(ScoutViewHolderBase viewHolder,
                                   ScoutMetric metric,
                                   int position) {
        super.populateViewHolder(viewHolder, metric, position);
        mCallback.onBind(viewHolder, position);
    }

    @Override
    public ScoutViewHolderBase onCreateViewHolder(ViewGroup parent, @MetricType int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case MetricType.BOOLEAN:
                return new CheckboxTemplateViewHolder(
                        inflater.inflate(R.layout.scout_template_checkbox, parent, false));
            case MetricType.NUMBER:
                return new CounterTemplateViewHolder(
                        inflater.inflate(R.layout.scout_template_counter, parent, false));
            case MetricType.TEXT:
                return new EditTextTemplateViewHolder(
                        inflater.inflate(R.layout.scout_template_notes, parent, false));
            case MetricType.LIST:
                return new SpinnerTemplateViewHolder(
                        inflater.inflate(R.layout.scout_template_spinner, parent, false));
            case MetricType.STOPWATCH:
                return new StopwatchTemplateViewHolder(
                        inflater.inflate(R.layout.scout_template_stopwatch, parent, false));
            case MetricType.HEADER:
                return new HeaderTemplateViewHolder(
                        inflater.inflate(R.layout.scout_template_header, parent, false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type,
                               DataSnapshot snapshot,
                               int index,
                               int oldIndex) {
        if (mCallback.onChildChanged(type, index)) {
            super.onChildChanged(type, snapshot, index, oldIndex);
        }
    }
}
