package com.supercilex.robotscouter.util.ui

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.supercilex.robotscouter.R

open class CardListHelper(
        private val adapter: FirestoreRecyclerAdapter<*, *>,
        private val recyclerView: RecyclerView
) {
    private val dividerSingle =
            ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider_single_item)!!
    private val dividerFirst =
            ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider_first_item)!!
    private val dividerMiddle =
            ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider_middle_item)!!
    private val dividerLast =
            ContextCompat.getDrawable(recyclerView.context, R.drawable.list_divider_last_item)!!

    fun onChildChanged(type: ChangeEventType, index: Int) {
        if (type == ChangeEventType.REMOVED) {
            recyclerView.notifyItemsChangedNoAnimation(index + if (index != 0) -1 else 0)
        }
    }

    fun onBind(viewHolder: RecyclerView.ViewHolder) {
        val position: Int = viewHolder.layoutPosition

        setBackground(viewHolder, position)

        recyclerView.post {
            if (position > adapter.itemCount - 1) return@post

            // Update the items above and below to ensure the correct corner configuration is shown
            val abovePos = position - 1
            val belowPos = position + 1

            recyclerView.findViewHolderForLayoutPosition(abovePos)?.let {
                setBackground(it, abovePos)
            }
            recyclerView.findViewHolderForLayoutPosition(belowPos)?.let {
                setBackground(it, belowPos)
            }
        }
    }

    protected open fun isFirstItem(position: Int) = position == 0

    protected open fun isLastItem(position: Int) = position == adapter.itemCount - 1

    private fun setBackground(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val itemView = viewHolder.itemView

        val paddingLeft = itemView.paddingLeft
        val paddingTop = itemView.paddingTop
        val paddingRight = itemView.paddingRight
        val paddingBottom = itemView.paddingBottom

        val isFirstItem = isFirstItem(position)
        val isLastItem = isLastItem(position)

        if (isFirstItem && isLastItem) {
            itemView.background = dividerSingle.constantState.newDrawable()
        } else if (isFirstItem) {
            itemView.background = dividerFirst.constantState.newDrawable()
        } else if (isLastItem) {
            itemView.background = dividerLast.constantState.newDrawable()
        } else {
            itemView.background = dividerMiddle.constantState.newDrawable()
        }

        itemView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }
}
