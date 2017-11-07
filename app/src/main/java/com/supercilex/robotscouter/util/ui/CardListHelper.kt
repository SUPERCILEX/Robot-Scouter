package com.supercilex.robotscouter.util.ui

import android.support.v7.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.supercilex.robotscouter.R

open class CardListHelper(
        private val adapter: FirestoreRecyclerAdapter<*, *>,
        private val recyclerView: RecyclerView
) {
    fun onChildChanged(type: ChangeEventType, index: Int) {
        if (type == ChangeEventType.REMOVED) {
            recyclerView.notifyItemsChangedNoAnimation(index + if (index != 0) -1 else 0)
        }
    }

    fun onBind(viewHolder: RecyclerView.ViewHolder) {
        val position: Int = viewHolder.layoutPosition

        viewHolder.setBackground(position)

        recyclerView.post {
            if (position > adapter.itemCount - 1) return@post

            // Update the items above and below to ensure the correct corner configuration is shown
            val abovePos = position - 1
            val belowPos = position + 1

            recyclerView.findViewHolderForLayoutPosition(abovePos)?.let {
                it.setBackground(abovePos)
            }
            recyclerView.findViewHolderForLayoutPosition(belowPos)?.let {
                it.setBackground(belowPos)
            }
        }
    }

    protected open fun isFirstItem(position: Int) = position == 0

    protected open fun isLastItem(position: Int) = position == adapter.itemCount - 1

    private fun RecyclerView.ViewHolder.setBackground(position: Int) {
        val paddingLeft = itemView.paddingLeft
        val paddingTop = itemView.paddingTop
        val paddingRight = itemView.paddingRight
        val paddingBottom = itemView.paddingBottom

        val isFirstItem = isFirstItem(position)
        val isLastItem = isLastItem(position)

        // It turns out performance is improved by parsing the drawable again b/c
        // setBackgroundResource can no-op if we are setting the same drawable instead of having
        // to recompute everything.
        itemView.setBackgroundResource(if (isFirstItem && isLastItem) {
            R.drawable.list_divider_single_item
        } else if (isFirstItem) {
            R.drawable.list_divider_first_item
        } else if (isLastItem) {
            R.drawable.list_divider_last_item
        } else {
            R.drawable.list_divider_middle_item
        })

        itemView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }
}
