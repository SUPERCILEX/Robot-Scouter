package com.supercilex.robotscouter.util.ui

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import androidx.core.view.postOnAnimationDelayed
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.lang.Math.max

private val defaultMaxAnimationDuration: Long by lazy {
    DefaultItemAnimator().maxAnimationDuration()
}

fun RecyclerView.isItemInRange(position: Int): Boolean = (layoutManager as LinearLayoutManager).let {
    val first = it.findFirstCompletelyVisibleItemPosition()

    // Only compute findLastCompletelyVisibleItemPosition if necessary
    position in first..(adapter.itemCount - 1)
            && position in first..it.findLastCompletelyVisibleItemPosition()
}

inline fun RecyclerView.notifyItemsNoChangeAnimation(
        update: RecyclerView.Adapter<*>.() -> Unit = {
            notifyItemRangeChanged(0, adapter.itemCount)
        }
) {
    val animator = itemAnimator as? SimpleItemAnimator

    animator?.supportsChangeAnimations = false
    adapter.update()

    postOnAnimationDelayed(animator.maxAnimationDuration()) {
        animator?.supportsChangeAnimations = true
    }
}

fun RecyclerView.ItemAnimator?.maxAnimationDuration() = this?.let {
    max(max(addDuration, removeDuration), changeDuration)
} ?: defaultMaxAnimationDuration

inline fun RecyclerView.Adapter<*>.swap(
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
        swap: (Int, Int) -> Unit
) {
    val fromPos = viewHolder.adapterPosition
    val toPos = target.adapterPosition

    if (fromPos < toPos) {
        for (i in fromPos until toPos) swap(i, i + 1) // Swap down
    } else {
        for (i in fromPos downTo toPos + 1) swap(i, i - 1) // Swap up
    }

    notifyItemMoved(fromPos, toPos)
}

/**
 * A [FirestoreRecyclerAdapter] whose state can be saved regardless of database connection
 * instability.
 *
 * This adapter will save its state across basic stop/start listening lifecycles, config changes,
 * and even full blown process death. Extenders _must_ call [SavedStateAdapter.onSaveInstanceState]
 * in the Activity/Fragment holding the adapter.
 */
abstract class SavedStateAdapter<T, VH : RecyclerView.ViewHolder>(
        options: FirestoreRecyclerOptions<T>,
        savedInstanceState: Bundle?,
        protected val recyclerView: RecyclerView
) : FirestoreRecyclerAdapter<T, VH>(options), Saveable {
    private var state: Parcelable?
    private val RecyclerView.state get() = layoutManager.onSaveInstanceState()

    init {
        state = savedInstanceState?.getParcelable(SAVED_STATE_KEY)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SAVED_STATE_KEY, recyclerView.state)
    }

    override fun stopListening() {
        state = recyclerView.state
        super.stopListening()
    }

    override fun onDataChanged() {
        recyclerView.layoutManager.onRestoreInstanceState(state)
        state = null
    }

    private companion object {
        const val SAVED_STATE_KEY = "layout_manager_saved_state"
    }
}
