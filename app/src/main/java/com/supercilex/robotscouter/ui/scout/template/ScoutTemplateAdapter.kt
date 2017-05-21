package com.supercilex.robotscouter.ui.scout.template

import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.firebase.ui.database.ChangeEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.metrics.MetricType
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric
import com.supercilex.robotscouter.ui.scout.ScoutAdapter
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase
import com.supercilex.robotscouter.ui.scout.viewholder.template.*

class ScoutTemplateAdapter(
        query: Query,
        manager: FragmentManager,
        recyclerView: RecyclerView,
        private val callback: ScoutTemplateItemTouchCallback) :
        ScoutAdapter(query, manager, recyclerView) {
    override fun populateViewHolder(
            viewHolder: ScoutViewHolderBase<*, *>,
            metric: ScoutMetric<Any>,
            position: Int) {
        super.populateViewHolder(viewHolder, metric, position)
        callback.onBind(viewHolder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, @MetricType viewType: Int):
            ScoutViewHolderBase<*, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        when (viewType) {
            MetricType.BOOLEAN -> return CheckboxTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_checkbox, parent, false))
            MetricType.NUMBER -> return CounterTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_counter, parent, false))
            MetricType.TEXT -> return EditTextTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_notes, parent, false))
            MetricType.LIST -> return SpinnerTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_spinner, parent, false))
            MetricType.STOPWATCH -> return StopwatchTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_stopwatch, parent, false))
            MetricType.HEADER -> return HeaderTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_header, parent, false))
            else -> throw IllegalStateException()
        }
    }

    override fun onChildChanged(
            type: ChangeEventListener.EventType,
            snapshot: DataSnapshot?,
            index: Int,
            oldIndex: Int) {
        if (callback.onChildChanged(type, index)) {
            super.onChildChanged(type, snapshot, index, oldIndex)
        }
    }
}
