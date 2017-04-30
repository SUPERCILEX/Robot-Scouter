package com.supercilex.robotscouter.ui;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase;

public class CardListHelper {
    private final FirebaseRecyclerAdapter mAdapter;

    public CardListHelper(FirebaseRecyclerAdapter adapter) {
        mAdapter = adapter;
    }

    public void onBind(ScoutViewHolderBase viewHolder, int position) {
        int paddingLeft = viewHolder.itemView.getPaddingLeft();
        int paddingTop = viewHolder.itemView.getPaddingTop();
        int paddingRight = viewHolder.itemView.getPaddingRight();
        int paddingBottom = viewHolder.itemView.getPaddingBottom();

        if (isFirstItem(position)) {
            viewHolder.itemView.setBackgroundResource(R.drawable.list_divider_first_item);
        } else if (isLastItem(position)) {
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
