package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.templates.R
import com.supercilex.robotscouter.shared.scouting.viewholder.StopwatchViewHolder
import kotlinx.android.synthetic.main.scout_template_base_reorder.*
import com.supercilex.robotscouter.R as RC

internal class StopwatchTemplateViewHolder(itemView: View) : StopwatchViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Stopwatch, List<Long>> {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText

    init {
        init()

        itemView as ConstraintLayout
        val set = ConstraintSet()
        set.clone(itemView)
        set.connect(RC.id.cycles, ConstraintSet.START, R.id.reorder, ConstraintSet.END, 0)
        set.applyTo(itemView)
    }
}
