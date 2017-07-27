package com.supercilex.robotscouter.ui.scouting

import android.support.annotation.CallSuper
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric

abstract class MetricViewHolderBase<FMetric : Metric<TMetric>, TMetric, out VView : TextView>(
        itemView: View) :
        RecyclerView.ViewHolder(itemView) {
    @Suppress("UNCHECKED_CAST")
    protected val name: VView = itemView.findViewById(R.id.name)
    protected lateinit var metric: FMetric
    protected lateinit var manager: FragmentManager
    private lateinit var animator: SimpleItemAnimator

    fun bind(metric: FMetric, manager: FragmentManager, animator: SimpleItemAnimator) {
        this.metric = metric
        this.manager = manager
        this.animator = animator

        bind()
    }

    @CallSuper
    protected open fun bind() {
        name.text = metric.name
    }

    protected fun updateMetricName(name: String) {
        if (!TextUtils.equals(metric.name, name)) {
            disableAnimations()
            metric.name = name
        }
    }

    protected fun updateMetricValue(value: TMetric) {
        if (value != metric.value) {
            disableAnimations()
            metric.value = value
        }
    }

    protected fun disableAnimations() {
        animator.supportsChangeAnimations = false
    }

    override fun toString(): String = metric.toString()
}
