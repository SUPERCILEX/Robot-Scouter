package com.supercilex.robotscouter.ui.scout.template;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class ScoutTemplateItemTouchCallback extends ItemTouchHelper.SimpleCallback {
    private ItemTouchCallback mCallback;

    public ScoutTemplateItemTouchCallback(ItemTouchCallback callback) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
        mCallback = callback;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return mCallback.onMove(recyclerView, viewHolder, target);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        mCallback.onSwiped(viewHolder, direction);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        mCallback.clearView(recyclerView, viewHolder);
    }
}
