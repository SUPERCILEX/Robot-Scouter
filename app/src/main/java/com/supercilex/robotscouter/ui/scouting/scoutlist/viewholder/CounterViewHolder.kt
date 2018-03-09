package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.support.annotation.CallSuper
import android.support.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.ui.scouting.scoutlist.CounterValueDialog
import com.supercilex.robotscouter.util.ui.setOnLongClickListenerCompat
import kotterknife.bindView

open class CounterViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Number, Long, TextView>(itemView),
        View.OnClickListener, View.OnLongClickListener {
    protected val count: TextView by bindView(R.id.count)
    private val increment: ImageButton by bindView(R.id.increment_counter)
    private val decrement: ImageButton by bindView(R.id.decrement_counter)

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

        if (id == R.id.increment_counter) {
            metric.value = ++value
        } else if (id == R.id.decrement_counter) {
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
