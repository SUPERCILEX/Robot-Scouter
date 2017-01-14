package com.supercilex.robotscouter.ui.scout.template;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public interface ItemTouchCallback {
    /**
     * @see ItemTouchHelper.SimpleCallback#onMove(RecyclerView, RecyclerView.ViewHolder, RecyclerView.ViewHolder)
     */
    boolean onMove(RecyclerView recyclerView,
                   RecyclerView.ViewHolder viewHolder,
                   RecyclerView.ViewHolder target);

    /**
     * @see ItemTouchHelper.SimpleCallback#onSwiped(RecyclerView.ViewHolder, int)
     */
    void onSwiped(RecyclerView.ViewHolder viewHolder, int direction);
}
