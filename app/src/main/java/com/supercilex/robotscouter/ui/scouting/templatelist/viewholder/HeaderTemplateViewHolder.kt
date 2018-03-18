package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.HeaderViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.android.synthetic.main.scout_template_base_reorder.*

class HeaderTemplateViewHolder(itemView: View) : HeaderViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Header, Nothing?> {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText

    init {
        init()
    }
}
