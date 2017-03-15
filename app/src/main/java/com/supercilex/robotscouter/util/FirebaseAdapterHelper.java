package com.supercilex.robotscouter.util;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class FirebaseAdapterHelper {
    private FirebaseAdapterHelper() {
        // no instance
    }

    public static <T> List<T> getItems(FirebaseRecyclerAdapter<T, ?> adapter) {
        List<T> items = new ArrayList<>(adapter.getItemCount());
        for (int i = 0; i < adapter.getItemCount(); i++) {
            items.add(adapter.getItem(i));
        }
        return items;
    }

    public static int getHighestIntPriority(List<DataSnapshot> snapshots) {
        int highest = 0;
        for (DataSnapshot snapshot : snapshots) {
            int priority = (int) ((double) snapshot.getPriority());
            if (priority > highest) highest = priority;
        }
        return highest;
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
