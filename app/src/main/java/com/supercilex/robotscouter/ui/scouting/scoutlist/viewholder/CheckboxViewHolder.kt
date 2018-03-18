package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.view.View
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import kotlinx.android.synthetic.main.scout_base_checkbox.*

open class CheckboxViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Boolean, Boolean>(itemView),
        View.OnClickListener {
    init {
        checkBox.setOnClickListener(this)
        name.setOnClickListener(this)
    }

    public override fun bind() {
        super.bind()
        checkBox.isChecked = metric.value
        checkBox.jumpDrawablesToCurrentState() // Skip animation on first load
    }

    override fun onClick(v: View) {
        if (v.id == R.id.checkBox) metric.value = checkBox.isChecked
        if (v.id == R.id.name) checkBox.performClick()
    }
}
