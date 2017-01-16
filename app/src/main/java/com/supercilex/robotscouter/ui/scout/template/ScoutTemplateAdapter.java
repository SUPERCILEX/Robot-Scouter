package com.supercilex.robotscouter.ui.scout.template;

import android.annotation.SuppressLint;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.MetricType;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.ui.scout.ScoutAdapter;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CheckboxTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.CounterTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.EditTextTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.ScoutTemplateViewHolder;
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerTemplateViewHolder;
import com.supercilex.robotscouter.util.BaseHelper;

import java.util.List;

public class ScoutTemplateAdapter extends ScoutAdapter implements ItemTouchCallback {
    private boolean mIsMovingItem;
    private int mScrollToPosition = -1;
    private View mRootView;
    private OnStartDragListener mDragListener;

    public ScoutTemplateAdapter(Class<ScoutMetric> modelClass,
                                Class<ScoutViewHolderBase> viewHolderClass,
                                Query query,
                                SimpleItemAnimator animator,
                                View rootView,
                                OnStartDragListener dragListener) {
        super(modelClass, viewHolderClass, query, animator);
        mRootView = rootView;
        mDragListener = dragListener;
    }

    @Override
    public void populateViewHolder(final ScoutViewHolderBase viewHolder,
                                   ScoutMetric metric,
                                   int position) {
        super.populateViewHolder(viewHolder, metric, position);

        viewHolder.itemView.findViewById(R.id.reorder)
                .setOnTouchListener(new View.OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                            viewHolder.itemView.clearFocus(); // Saves data
                            mDragListener.startDrag(viewHolder);
                        }
                        return false;
                    }
                });

        if (position == mScrollToPosition) {
            ((ScoutTemplateViewHolder) viewHolder).requestFocus();
            mScrollToPosition = -1;
        }
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
            case MetricType.NOTE:
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
        if (type == ChangeEventListener.EventType.MOVED) {
            if (!mIsMovingItem) super.onChildChanged(type, index, oldIndex);
            return;
        } else if (type == ChangeEventListener.EventType.ADDED && index == mScrollToPosition) {
            ((RecyclerView) mRootView.findViewById(R.id.list)).scrollToPosition(mScrollToPosition);
        }
        super.onChildChanged(type, index, oldIndex);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        mIsMovingItem = true;
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();

        notifyItemMoved(fromPos, toPos);

        List<DataSnapshot> snapshots = getSnapshots();
        snapshots.add(toPos, snapshots.remove(fromPos));
        for (int i = 0; i < snapshots.size(); i++) {
            snapshots.get(i).getRef().setPriority(i);
        }

        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();
        final ScoutMetric deletedMetric = getItem(position);
        final DatabaseReference deletedRef = getRef(position);

        viewHolder.itemView.clearFocus(); // Needed to prevent the item from being re-added
        deletedRef.removeValue();

        BaseHelper.showSnackbar(
                mRootView,
                R.string.deleted,
                Snackbar.LENGTH_LONG,
                R.string.undo,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deletedRef.setValue(deletedMetric, position);
                    }
                });
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        mIsMovingItem = false;
    }

    public void addItemToScrollQueue(int position) {
        mScrollToPosition = position;
    }
}
