package com.supercilex.robotscouter.util.ui

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import org.jetbrains.anko.bundleOf
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

fun RecyclerView.notifyItemsChangedNoAnimation(position: Int, itemCount: Int) {
    val animator: RecyclerView.ItemAnimator? = itemAnimator

    itemAnimator = null
    adapter.notifyItemRangeChanged(position, itemCount)

    ViewCompat.postOnAnimationDelayed(
            this,
            { itemAnimator = animator },
            animator.maxAnimationDuration()
    )
}

inline fun RecyclerView.notifyItemsNoChangeAnimation(update: RecyclerView.Adapter<*>.() -> Unit) {
    val animator = itemAnimator as SimpleItemAnimator?

    animator?.supportsChangeAnimations = false
    adapter.update()

    ViewCompat.postOnAnimationDelayed(
            this,
            { animator?.supportsChangeAnimations = true },
            animator.maxAnimationDuration()
    )
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
) : FirestoreRecyclerAdapter<T, VH>(options) {
    private var state: Parcelable?

    init {
        state = savedInstanceState?.getParcelable(SAVED_STATE_KEY)
    }

    /**
     * @see [android.support.v7.app.AppCompatActivity.onSaveInstanceState]
     * @see [android.support.v4.app.Fragment.onSaveInstanceState]
     */
    fun onSaveInstanceState() = bundleOf(SAVED_STATE_KEY to _onSaveInstanceState())

    private fun _onSaveInstanceState(): Parcelable =
            recyclerView.layoutManager.onSaveInstanceState()

    override fun stopListening() {
        state = _onSaveInstanceState()
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
