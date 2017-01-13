package com.supercilex.robotscouter.ui.scout.template;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
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

import java.util.ArrayList;
import java.util.List;

public class ScoutTemplateAdapter extends ScoutAdapter {
    private Query mQuery;
    private List<Integer> mChangeQueue = new ArrayList<>();
    private List<Pair<Integer, Integer>> mMoveQueue = new ArrayList<>();

    public ScoutTemplateAdapter(Class<ScoutMetric> modelClass,
                                Class<ScoutViewHolderBase> viewHolderClass,
                                Query query,
                                SimpleItemAnimator animator) {
        super(modelClass, viewHolderClass, query, animator);
        mQuery = query;
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

    @Override
    public void onChildChanged(ChangeEventListener.EventType type, int index, int oldIndex) {
        if (!mChangeQueue.isEmpty() && mChangeQueue.remove(0) == index) {
            if (!mMoveQueue.isEmpty() && index == mMoveQueue.get(0).first) {
                Pair<Integer, Integer> movement = mMoveQueue.remove(0);
                notifyItemMoved(movement.first, movement.second);
            }
        } else {
            super.onChildChanged(type, index, oldIndex);
        }
    }

    public boolean onMove(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();
        mMoveQueue.add(new Pair<>(fromPos, toPos));
        List<ScoutMetric> metrics = getItems();
        metrics.add(toPos, metrics.remove(fromPos));

        if (toPos < fromPos) { // Moving item up
            for (int i = toPos; i <= fromPos; i++) {
                setItem(i, metrics.get(i));
            }
        } else {
            for (int i = fromPos; i <= toPos; i++) {
                setItem(i, metrics.get(i));
            }
        }

        return true;
    }

    private void setItem(int position, ScoutMetric metric) {
        mQuery.getRef().child(String.valueOf(position)).setValue(metric);
        mChangeQueue.add(position);
    }
}
