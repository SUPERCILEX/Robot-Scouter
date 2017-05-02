package com.supercilex.robotscouter.ui;

import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;

public class CardListHelper {
    private final FirebaseRecyclerAdapter mAdapter;
    private final RecyclerView mRecyclerView;

    private final boolean mHasSafeCorners;

    public CardListHelper(FirebaseRecyclerAdapter adapter,
                          RecyclerView recyclerView,
                          boolean hasSafeCorners) {
        mAdapter = adapter;
        mRecyclerView = recyclerView;
        mHasSafeCorners = hasSafeCorners;
    }

    public void onBind(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getLayoutPosition();

        setBackground(viewHolder, position);


        if (mHasSafeCorners) return;

        // Update the items above and below to ensure the correct corner configuration is shown
        int abovePos = position - 1;
        int belowPos = position + 1;
        RecyclerView.ViewHolder above = mRecyclerView.findViewHolderForLayoutPosition(abovePos);
        RecyclerView.ViewHolder below = mRecyclerView.findViewHolderForLayoutPosition(belowPos);

        if (above != null) setBackground(above, abovePos);
        if (below != null) setBackground(below, belowPos);
    }

    protected boolean isFirstItem(int position) {
        return position == 0;
    }

    protected boolean isLastItem(int position) {
        return position == mAdapter.getItemCount() - 1;
    }

    private void setBackground(RecyclerView.ViewHolder viewHolder, int position) {
        int paddingLeft = viewHolder.itemView.getPaddingLeft();
        int paddingTop = viewHolder.itemView.getPaddingTop();
        int paddingRight = viewHolder.itemView.getPaddingRight();
        int paddingBottom = viewHolder.itemView.getPaddingBottom();

        boolean isFirstItem = isFirstItem(position);
        boolean isLastItem = isLastItem(position);

        if (isFirstItem && isLastItem) {
            viewHolder.itemView.setBackgroundResource(R.drawable.list_divider_single_item);
        } else if (isFirstItem) {
            viewHolder.itemView.setBackgroundResource(R.drawable.list_divider_first_item);
        } else if (isLastItem) {
            viewHolder.itemView.setBackgroundResource(R.drawable.list_divider_last_item);
        } else {
            viewHolder.itemView.setBackgroundResource(R.drawable.list_divider_middle_item);
        }

        viewHolder.itemView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }
}
