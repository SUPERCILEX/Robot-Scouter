package com.supercilex.robotscouter.ui

import android.support.v7.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.supercilex.robotscouter.R

open class CardListHelper(private val adapter: FirebaseRecyclerAdapter<*, *>,
                          private val recyclerView: RecyclerView,
                          private val hasSafeCorners: Boolean = false) {
    fun onBind(viewHolder: RecyclerView.ViewHolder) {
        val position: Int = viewHolder.layoutPosition

        setBackground(viewHolder, position)


        if (hasSafeCorners) return

        recyclerView.post {
            if (position > adapter.itemCount - 1) return@post

            // Update the items above and below to ensure the correct corner configuration is shown
            val abovePos = position - 1
            val belowPos = position + 1
            val above: RecyclerView.ViewHolder? = recyclerView.findViewHolderForLayoutPosition(abovePos)
            val below: RecyclerView.ViewHolder? = recyclerView.findViewHolderForLayoutPosition(belowPos)

            above?.let { setBackground(it, abovePos) }
            below?.let { setBackground(it, belowPos) }
        }
    }

    protected open fun isFirstItem(position: Int): Boolean {
        return position == 0
    }

    protected open fun isLastItem(position: Int): Boolean {
        return position == adapter.itemCount - 1
    }

    private fun setBackground(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val itemView = viewHolder.itemView

        val paddingLeft = itemView.paddingLeft
        val paddingTop = itemView.paddingTop
        val paddingRight = itemView.paddingRight
        val paddingBottom = itemView.paddingBottom

        val isFirstItem = isFirstItem(position)
        val isLastItem = isLastItem(position)

        if (isFirstItem && isLastItem) {
            itemView.setBackgroundResource(R.drawable.list_divider_single_item)
        } else if (isFirstItem) {
            itemView.setBackgroundResource(R.drawable.list_divider_first_item)
        } else if (isLastItem) {
            itemView.setBackgroundResource(R.drawable.list_divider_last_item)
        } else {
            itemView.setBackgroundResource(R.drawable.list_divider_middle_item)
        }

        itemView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }
}
