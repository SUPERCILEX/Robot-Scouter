package com.supercilex.robotscouter.util.ui

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import java.lang.Math.max

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
