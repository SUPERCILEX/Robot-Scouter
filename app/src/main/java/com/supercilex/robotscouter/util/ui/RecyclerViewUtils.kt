package com.supercilex.robotscouter.util.ui

import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import java.lang.Math.max

fun RecyclerView.isItemInRange(position: Int): Boolean = (layoutManager as LinearLayoutManager).let {
    val first = it.findFirstCompletelyVisibleItemPosition()

    // Only compute findLastCompletelyVisibleItemPosition if necessary
    position in first..(adapter.itemCount - 1)
            && position in first..it.findLastCompletelyVisibleItemPosition()
}

fun RecyclerView.ItemAnimator.maxAnimationDuration() =
        max(max(addDuration, removeDuration), changeDuration)

fun RecyclerView.notifyItemsChangedNoAnimation(position: Int, itemCount: Int = 1) {
    val animator = itemAnimator as SimpleItemAnimator

    animator.supportsChangeAnimations = false
    adapter.notifyItemRangeChanged(position, itemCount)

    ViewCompat.postOnAnimationDelayed(
            this,
            { animator.supportsChangeAnimations = true },
            animator.maxAnimationDuration()
    )
}
