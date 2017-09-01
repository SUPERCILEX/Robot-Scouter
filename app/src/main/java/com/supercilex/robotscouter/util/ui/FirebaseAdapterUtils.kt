package com.supercilex.robotscouter.util.ui

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DataSnapshot
import kotlin.math.max

fun <T> getAdapterItems(adapter: FirebaseRecyclerAdapter<T, *>): List<T> =
        (0 until adapter.itemCount).map { adapter.getItem(it) }

fun getHighestIntPriority(snapshots: List<DataSnapshot>): Int =
        snapshots.map { ((it.priority ?: return@map 0) as Double).toInt() }.max() ?: 0

fun maxAnimationDuration(animator: RecyclerView.ItemAnimator) =
        max(max(animator.addDuration, animator.removeDuration),
            max(animator.changeDuration, animator.changeDuration))

fun notifyItemsChangedNoAnimation(recyclerView: RecyclerView,
                                  position: Int,
                                  itemCount: Int = 1) {
    val animator = recyclerView.itemAnimator as SimpleItemAnimator

    animator.supportsChangeAnimations = false
    recyclerView.adapter.notifyItemRangeChanged(position, itemCount)

    ViewCompat.postOnAnimationDelayed(
            recyclerView,
            { animator.supportsChangeAnimations = true },
            maxAnimationDuration(animator))
}
