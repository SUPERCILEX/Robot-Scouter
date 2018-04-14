package com.supercilex.robotscouter.shared.scouting.viewholder

import android.support.v7.widget.RecyclerView
import android.view.View
import androidx.core.view.updateLayoutParams
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.shared.scouting.R

open class HeaderViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Header, Nothing?>(itemView) {
    private val topMargin =
            itemView.resources.getDimension(R.dimen.list_item_padding_vertical_within).toInt()

    override fun bind() {
        super.bind()
        itemView.updateLayoutParams<RecyclerView.LayoutParams> {
            topMargin = if (layoutPosition == 0) 0 else this@HeaderViewHolder.topMargin
        }
    }
}
