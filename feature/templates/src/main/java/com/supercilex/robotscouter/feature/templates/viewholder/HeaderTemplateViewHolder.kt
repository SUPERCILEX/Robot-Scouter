package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.templates.R
import com.supercilex.robotscouter.shared.scouting.viewholder.HeaderViewHolder

internal class HeaderTemplateViewHolder(itemView: View) : HeaderViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Header, Nothing?> {
    override val reorderView: ImageView by unsafeLazy { itemView.findViewById(R.id.reorder) }
    override val nameEditor = name as EditText

    init {
        init()
    }
}
