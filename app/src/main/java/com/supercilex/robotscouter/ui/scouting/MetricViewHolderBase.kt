package com.supercilex.robotscouter.ui.scouting

import android.support.annotation.CallSuper
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import kotterknife.bindView

abstract class MetricViewHolderBase<M : Metric<T>, T, out V : TextView>(
        itemView: View
) : RecyclerView.ViewHolder(itemView) {
    lateinit var metric: M
        private set

    protected val name: V by bindView(R.id.name)
    protected lateinit var fragmentManager: FragmentManager
        private set

    fun bind(metric: M, manager: FragmentManager) {
        this.metric = metric
        this.fragmentManager = manager

        bind()
    }

    @CallSuper
    protected open fun bind() {
        name.text = metric.name
    }
}
