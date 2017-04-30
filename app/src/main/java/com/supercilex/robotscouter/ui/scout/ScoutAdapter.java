package com.supercilex.robotscouter.ui.scout;

import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.metrics.MetricType;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.HeaderViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.StopwatchViewHolder;

public class ScoutAdapter extends FirebaseRecyclerAdapter<ScoutMetric, ScoutViewHolderBase> {
    private final FragmentManager mManager;
    private final SimpleItemAnimator mAnimator;

    public ScoutAdapter(Query query, FragmentManager manager, SimpleItemAnimator animator) {
        super(ScoutUtils.METRIC_PARSER, 0, ScoutViewHolderBase.class, query);
        mManager = manager;
        mAnimator = animator;
    }

    @Override
    public void populateViewHolder(ScoutViewHolderBase viewHolder,
                                   ScoutMetric metric,
                                   int position) {
        mAnimator.setSupportsChangeAnimations(true);

        if (metric.getType() != MetricType.HEADER) {
            int paddingLeft = viewHolder.itemView.getPaddingLeft();
            int paddingTop = viewHolder.itemView.getPaddingTop();
            int paddingRight = viewHolder.itemView.getPaddingRight();
            int paddingBottom = viewHolder.itemView.getPaddingBottom();

            if (position == getItemCount() - 1
                    || getItem(position + 1).getType() == MetricType.HEADER) {
                viewHolder.itemView.setBackgroundResource(R.drawable.list_divider_last_item);
            } else {
                viewHolder.itemView.setBackgroundResource(R.drawable.list_divider);
            }

            viewHolder.itemView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }

        //noinspection unchecked
        viewHolder.bind(metric, mManager, mAnimator);
    }

    @Override
    public ScoutViewHolderBase onCreateViewHolder(ViewGroup parent, @MetricType int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case MetricType.BOOLEAN:
                return new CheckboxViewHolder(
                        inflater.inflate(R.layout.scout_checkbox, parent, false));
            case MetricType.NUMBER:
                return new CounterViewHolder(
                        inflater.inflate(R.layout.scout_counter, parent, false));
            case MetricType.TEXT:
                return new EditTextViewHolder(
                        inflater.inflate(R.layout.scout_notes, parent, false));
            case MetricType.LIST:
                return new SpinnerViewHolder(
                        inflater.inflate(R.layout.scout_spinner, parent, false));
            case MetricType.STOPWATCH:
                return new StopwatchViewHolder(
                        inflater.inflate(R.layout.scout_stopwatch, parent, false));
            case MetricType.HEADER:
                return new HeaderViewHolder(
                        inflater.inflate(R.layout.scout_header, parent, false));
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    @MetricType
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }
}
