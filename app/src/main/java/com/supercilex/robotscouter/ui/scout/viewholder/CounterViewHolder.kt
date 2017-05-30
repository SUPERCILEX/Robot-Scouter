package com.supercilex.robotscouter.ui.scout.viewholder

import android.support.annotation.CallSuper
import android.support.transition.TransitionManager
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.util.FIREBASE_VALUE

open class CounterViewHolder(itemView: View) :
        ScoutViewHolderBase<Metric.Number, Int, TextView>(itemView), View.OnClickListener, View.OnLongClickListener {
    protected val count: TextView = itemView.findViewById(R.id.count)
    private val increment: ImageButton = itemView.findViewById(R.id.increment_counter)
    private val decrement: ImageButton = itemView.findViewById(R.id.decrement_counter)

    private val stringWithoutUnit: String get() {
        val unit: String? = metric.unit
        val count = count.text.toString()
        return if (TextUtils.isEmpty(unit)) count else count.replace(unit!!, "")
    }

    public override fun bind() {
        super.bind()
        setValue(metric.value)
        increment.setOnClickListener(this)
        increment.setOnLongClickListener(this)
        decrement.setOnClickListener(this)
        decrement.setOnLongClickListener(this)
    }

    @CallSuper
    override fun onClick(v: View) {
        val id = v.id
        var value = stringWithoutUnit.toInt()
        if (id == R.id.increment_counter) {
            updateValue(++value)
        } else if (id == R.id.decrement_counter && value > 0) { // no negative values
            updateValue(--value)
        }
    }

    protected open fun setValue(value: Int) {
        val unit: String? = metric.unit
        count.text = if (TextUtils.isEmpty(unit)) value.toString() else value.toString() + unit!!
    }

    private fun updateValue(value: Int) {
        TransitionManager.beginDelayedTransition(itemView as ViewGroup)
        setValue(value)
        updateMetricValue(value)
    }

    override fun onLongClick(v: View): Boolean {
        ScoutCounterValueDialog.show(manager, metric.ref.child(FIREBASE_VALUE), stringWithoutUnit)
        return true
    }
}
