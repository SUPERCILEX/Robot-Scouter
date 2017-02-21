package com.supercilex.robotscouter.util;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.List;

public final class FirebaseAdapterHelper {
    private FirebaseAdapterHelper() {
        // no instance
    }

    public static List<DatabaseReference> getRefs(FirebaseRecyclerAdapter adapter) {
        List<DatabaseReference> refs = new ArrayList<>(adapter.getItemCount());
        for (int i = 0; i < adapter.getItemCount(); i++) {
            refs.add(adapter.getRef(i));
        }
        return refs;
    }

    public static <T> List<T> getItems(FirebaseRecyclerAdapter<T, ?> adapter) {
        List<T> items = new ArrayList<>(adapter.getItemCount());
        for (int i = 0; i < adapter.getItemCount(); i++) {
            items.add(adapter.getItem(i));
        }
        return items;
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
