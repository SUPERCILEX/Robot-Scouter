package com.supercilex.robotscouter.shared

import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.supercilex.robotscouter.core.ui.CardMetric
import com.supercilex.robotscouter.core.ui.notifyItemsNoChangeAnimation

open class CardListHelper(
        private val adapter: FirestoreRecyclerAdapter<*, *>,
        private val recyclerView: RecyclerView
) {
    fun onChildChanged(type: ChangeEventType, index: Int) {
        if (type == ChangeEventType.REMOVED) {
            recyclerView.notifyItemsNoChangeAnimation {
                notifyItemRangeChanged(index + if (index == 0) 0 else -1, 1)
            }
        }
    }

    fun onBind(viewHolder: RecyclerView.ViewHolder, position: Int) {
        viewHolder.setBackground(position)

        // Update the items above and below to ensure the correct corner configuration is shown
        val rv = recyclerView
        val abovePos = position - 1
        val belowPos = position + 1
        rv.post {
            rv.findViewHolderForLayoutPosition(abovePos)?.setBackground(abovePos)
            rv.findViewHolderForLayoutPosition(belowPos)?.setBackground(belowPos)
        }
    }

    protected open fun isFirstItem(position: Int) = position == 0

    protected open fun isLastItem(position: Int) = position == adapter.itemCount - 1

    private fun RecyclerView.ViewHolder.setBackground(position: Int) {
        try {
            (itemView as CardMetric).apply {
                isFirstItem = isFirstItem(position)
                isLastItem = isLastItem(position)
            }
        } catch (e: IndexOutOfBoundsException) {
            // Ideally, we'd like to check whether or not `position == adapterPosition`, but getting
            // the adapter position is an expensive computation. Instead, we use a try block which
            // is way cheaper and reserve the expensive catch for the rare IOOBE.
        }
    }
}
