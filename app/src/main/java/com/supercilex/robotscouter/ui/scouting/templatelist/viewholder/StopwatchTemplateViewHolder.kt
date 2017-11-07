package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.StopwatchViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class StopwatchTemplateViewHolder(itemView: View) : StopwatchViewHolder(itemView),
        MetricTemplateViewHolder<Metric<List<Long>>, List<Long>> {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    init {
        init()

        itemView as ConstraintLayout
        val set = ConstraintSet()
        set.clone(itemView)
        set.connect(R.id.list, ConstraintSet.START, R.id.reorder, ConstraintSet.END, 0)
        set.applyTo(itemView)
    }
}
