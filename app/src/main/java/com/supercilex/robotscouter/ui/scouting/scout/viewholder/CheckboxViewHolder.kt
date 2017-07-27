package com.supercilex.robotscouter.ui.scouting.scout.viewholder

import android.support.annotation.CallSuper
import android.view.View
import android.widget.CheckBox
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase

open class CheckboxViewHolder(itemView: View) :
        MetricViewHolderBase<Metric.Boolean, Boolean, CheckBox>(itemView),
        View.OnClickListener {
    public override fun bind() {
        super.bind()
        name.isChecked = metric.value
        name.setOnClickListener(this)
    }

    @CallSuper
    override fun onClick(v: View) = updateMetricValue(name.isChecked)
}
