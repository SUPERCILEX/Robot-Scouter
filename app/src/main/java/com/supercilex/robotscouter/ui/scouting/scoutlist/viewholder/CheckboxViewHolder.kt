package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import kotterknife.bindView

open class CheckboxViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Boolean, Boolean, TextView>(itemView),
        View.OnClickListener {
    protected val checkbox: CheckBox by bindView(R.id.checkbox)

    public override fun bind() {
        super.bind()
        checkbox.apply {
            isChecked = metric.value
            jumpDrawablesToCurrentState() // Skip animation on first load
            setOnClickListener(this@CheckboxViewHolder)
        }
        name.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.checkbox) metric.value = checkbox.isChecked
        if (v.id == R.id.name) checkbox.performClick()
    }
}
