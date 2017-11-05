package com.supercilex.robotscouter.ui.scouting

import android.support.annotation.CallSuper
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import kotterknife.bindView

abstract class MetricViewHolderBase<FMetric : Metric<TMetric>, TMetric, out VView : TextView>(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {
    lateinit var metric: FMetric
        private set

    protected val name: VView by bindView(R.id.name)
    protected lateinit var manager: FragmentManager
        private set

    fun bind(metric: FMetric, manager: FragmentManager) {
        this.metric = metric
        this.manager = manager

        bind()
    }

    @CallSuper
    protected open fun bind() {
        name.text = metric.name
    }
}
