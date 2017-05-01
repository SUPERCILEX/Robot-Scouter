package com.supercilex.robotscouter.ui;

import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;

public class CardListHelper {
    private final FirebaseRecyclerAdapter mAdapter;

    public CardListHelper(FirebaseRecyclerAdapter adapter) {
        mAdapter = adapter;
    }

    public void onBind(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getLayoutPosition();

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

    public boolean isFirstItem(int position) {
        return position == 0;
    }

    protected boolean isLastItem(int position) {
        return position == mAdapter.getItemCount() - 1;
    }
}
