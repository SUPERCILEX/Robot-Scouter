package com.supercilex.robotscouter.ui.scout;

import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.adapter.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;

public class ScoutAdapter extends FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolderBase> {
    private FragmentManager mManager;
    private SimpleItemAnimator mAnimator;

    public ScoutAdapter(Class<ScoutMetric> modelClass,
                        Class<ScoutViewHolderBase> viewHolderClass,
                        Query query,
                        FragmentManager manager,
                        SimpleItemAnimator animator) {
        super(modelClass, 0, viewHolderClass, query);
        mManager = manager;
        mAnimator = animator;
    }

    @Override
    public void populateViewHolder(ScoutViewHolderBase viewHolder,
                                   ScoutMetric metric,
                                   int position) {
        //noinspection unchecked
        viewHolder.bind(metric, getRef(position), mManager, mAnimator);
        mAnimator.setSupportsChangeAnimations(true);
    }

    @Override
    public ScoutViewHolderBase onCreateViewHolder(ViewGroup parent, @MetricType int viewType) {
        switch (viewType) {
            case MetricType.CHECKBOX:
                return new CheckboxViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_checkbox, parent, false));
            case MetricType.COUNTER:
                return new CounterViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_counter, parent, false));
            case MetricType.NOTE:
                return new EditTextViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_notes, parent, false));
            case MetricType.SPINNER:
                return new SpinnerViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.scout_spinner, parent, false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public ScoutMetric parseSnapshot(DataSnapshot snapshot) {
        return ScoutUtils.getMetric(snapshot);
    }

    @Override
    @MetricType
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }
}
