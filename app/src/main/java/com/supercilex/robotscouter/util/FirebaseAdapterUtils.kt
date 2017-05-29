package com.supercilex.robotscouter.util

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DataSnapshot

fun <T> getAdapterItems(adapter: FirebaseRecyclerAdapter<T, *>): List<T> =
        (0 until adapter.itemCount).map { adapter.getItem(it) }

fun getHighestIntPriority(snapshots: List<DataSnapshot>): Int =
        snapshots.map { ((it.priority ?: return@map 0) as Double).toInt() }.max() ?: 0

fun saveRecyclerViewState(outState: Bundle, layoutManager: RecyclerView.LayoutManager) =
        outState.putParcelable(MANAGER_STATE, layoutManager.onSaveInstanceState())

fun restoreRecyclerViewState(savedInstanceState: Bundle?,
                             layoutManager: RecyclerView.LayoutManager) {
    if (savedInstanceState == null) return
    layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable<Parcelable>(MANAGER_STATE))
}

fun notifyAllItemsChangedNoAnimation(recyclerView: RecyclerView, adapter: RecyclerView.Adapter<*>) {
    val animator = recyclerView.itemAnimator as SimpleItemAnimator

    animator.supportsChangeAnimations = false
    adapter.notifyItemRangeChanged(0, adapter.itemCount + 1)

    recyclerView.post { animator.supportsChangeAnimations = true }
}
