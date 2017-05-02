package com.supercilex.robotscouter.ui.scout.template;

import android.support.design.widget.Snackbar;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.scout.viewholder.template.ScoutTemplateViewHolder;
import com.supercilex.robotscouter.util.FirebaseAdapterUtils;

public class ScoutTemplateItemTouchCallback<T, VH extends RecyclerView.ViewHolder> extends ItemTouchHelper.SimpleCallback {
    private final View mRootView;
    private final RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<T, VH> mAdapter;
    private ItemTouchHelper mItemTouchHelper;

    private int mScrollToPosition;
    private boolean mIsItemMoving;

    public ScoutTemplateItemTouchCallback(View rootView) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
        mRootView = rootView;
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.list);
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        mItemTouchHelper = itemTouchHelper;
    }

    public void setAdapter(FirebaseRecyclerAdapter<T, VH> adapter) {
        mAdapter = adapter;
    }

    public void onBind(RecyclerView.ViewHolder viewHolder, int position) {
        viewHolder.itemView.findViewById(R.id.reorder)
                .setOnTouchListener((v, event) -> {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        viewHolder.itemView.clearFocus(); // Saves data
                        mItemTouchHelper.startDrag(viewHolder);
                        v.performClick();
                        return true;
                    }
                    return false;
                });

        if (position == mScrollToPosition) {
            ((ScoutTemplateViewHolder) viewHolder).requestFocus();
            mScrollToPosition = RecyclerView.NO_POSITION;
        }
    }

    public void addItemToScrollQueue(int position) {
        mScrollToPosition = position;
    }

    public boolean onChildChanged(ChangeEventListener.EventType type, int index) {
        if (mIsItemMoving) {
            return type == ChangeEventListener.EventType.MOVED;
        } else if (type == ChangeEventListener.EventType.ADDED && index == mScrollToPosition) {
            mRecyclerView.scrollToPosition(mScrollToPosition);
        }
        return true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();
        mIsItemMoving = true;

        mAdapter.getRef(fromPos).setPriority(toPos);
        mAdapter.getRef(toPos).setPriority(fromPos);

        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();
        final DatabaseReference deletedRef = mAdapter.getRef(position);

        viewHolder.itemView.clearFocus(); // Needed to prevent the item from being re-added
        deletedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                deletedRef.removeValue();

                Snackbar.make(mRootView, R.string.deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo,
                                   v -> deletedRef.setValue(snapshot.getValue(), position))
                        .show();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                FirebaseCrash.report(error.toException());
            }
        });
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mIsItemMoving = false;
        FirebaseAdapterUtils.notifyAllItemsChangedNoAnimation(mRecyclerView, mAdapter);
    }
}
