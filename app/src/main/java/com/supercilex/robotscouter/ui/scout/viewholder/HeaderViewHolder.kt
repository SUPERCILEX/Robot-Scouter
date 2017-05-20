package com.supercilex.robotscouter.ui.scout.viewholder

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R

open class HeaderViewHolder(itemView: View) : ScoutViewHolderBase<String, TextView>(itemView) {
    override fun bind() {
        super.bind()
        if (layoutPosition != 0) {
            val params = itemView.layoutParams as RecyclerView.LayoutParams
            params.topMargin = itemView.resources.getDimension(R.dimen.list_item_padding_vertical_within).toInt()
        }
    }
}
