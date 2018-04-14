package com.supercilex.robotscouter.shared.scouting.viewholder

import android.support.annotation.CallSuper
import android.support.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.shared.scouting.CounterValueDialog
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.shared.scouting.R
import kotlinx.android.synthetic.main.scout_base_counter.*

open class CounterViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Number, Long>(itemView),
        View.OnClickListener, View.OnLongClickListener {
    protected open val valueWithoutUnit: String
        get() {
            val unit: String? = metric.unit
            val count = count.text.toString()
            return if (unit?.isNotBlank() == true) count.removeSuffix(unit) else count
        }

    init {
        increment.setOnClickListener(this)
        decrement.setOnClickListener(this)
        count.setOnLongClickListenerCompat(this)
    }

    public override fun bind() {
        super.bind()
        update()
    }

    private fun update() {
        setValue()
        decrement.isEnabled = metric.value > 0 // No negative values
    }

    @CallSuper
    override fun onClick(v: View) {
        val id = v.id
        var value = valueWithoutUnit.toLong()

        if (id == R.id.increment) {
            metric.value = ++value
        } else if (id == R.id.decrement) {
            metric.value = --value
        }

        TransitionManager.beginDelayedTransition(itemView as ViewGroup)
        update()
    }

    protected open fun setValue() {
        val value = metric.value.toString()
        val unit: String? = metric.unit
        count.text = if (unit?.isNotBlank() == true) value + unit else value
    }

    override fun onLongClick(v: View): Boolean {
        CounterValueDialog.show(fragmentManager, metric.ref, valueWithoutUnit)
        return true
    }
}
