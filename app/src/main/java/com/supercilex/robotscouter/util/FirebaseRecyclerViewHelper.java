package com.supercilex.robotscouter.util;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;

public final class FirebaseRecyclerViewHelper {
    private FirebaseRecyclerViewHelper() {
        // no instance
    }

    public static void restoreRecyclerViewState(Bundle savedInstanceState,
                                                final RecyclerView.Adapter adapter,
                                                final RecyclerView.LayoutManager layoutManager) {
        if (savedInstanceState != null && adapter != null && layoutManager != null) {
            final Parcelable managerState = savedInstanceState.getParcelable(Constants.MANAGER_STATE);
            final int count = savedInstanceState.getInt(Constants.ITEM_COUNT);
            if (adapter.getItemCount() >= count) {
                layoutManager.onRestoreInstanceState(managerState);
            } else {
                adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        if (adapter.getItemCount() >= count) {
                            layoutManager.onRestoreInstanceState(managerState);
                            adapter.unregisterAdapterDataObserver(this);
                        }
                    }
                });
            }
        }
    }

    public static void saveRecyclerViewState(Bundle outState,
                                             RecyclerView.Adapter adapter,
                                             RecyclerView.LayoutManager layoutManager) {
        if (adapter != null) {
            outState.putParcelable(Constants.MANAGER_STATE, layoutManager.onSaveInstanceState());
            outState.putInt(Constants.ITEM_COUNT, adapter.getItemCount());
        }
    }
}
