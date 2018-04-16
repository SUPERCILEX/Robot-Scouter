package com.supercilex.robotscouter.feature.templates

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.MetricType
import com.supercilex.robotscouter.feature.templates.viewholder.CheckboxTemplateViewHolder
import com.supercilex.robotscouter.feature.templates.viewholder.CounterTemplateViewHolder
import com.supercilex.robotscouter.feature.templates.viewholder.EditTextTemplateViewHolder
import com.supercilex.robotscouter.feature.templates.viewholder.HeaderTemplateViewHolder
import com.supercilex.robotscouter.feature.templates.viewholder.SpinnerTemplateViewHolder
import com.supercilex.robotscouter.feature.templates.viewholder.StopwatchTemplateViewHolder
import com.supercilex.robotscouter.shared.scouting.MetricListAdapterBase
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase

internal class TemplateAdapter(
        metrics: ObservableSnapshotArray<Metric<*>>,
        owner: LifecycleOwner,
        manager: FragmentManager,
        recyclerView: RecyclerView,
        savedInstanceState: Bundle?,
        private val callback: TemplateItemTouchCallback<Metric<*>>
) : MetricListAdapterBase(
        FirestoreRecyclerOptions.Builder<Metric<*>>()
                .setSnapshotArray(metrics)
                .setLifecycleOwner(owner)
                .build(),
        recyclerView,
        savedInstanceState,
        manager
) {
    override fun getItem(position: Int) = callback.getItem(position)

    override fun getItemCount() = callback.getItemCount { super.getItemCount() }

    override fun onBindViewHolder(
            viewHolder: MetricViewHolderBase<*, *>,
            position: Int,
            metric: Metric<*>
    ) {
        super.onBindViewHolder(viewHolder, position, metric)
        callback.onBind(viewHolder, position)
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): MetricViewHolderBase<*, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (MetricType.valueOf(viewType)) {
            MetricType.HEADER -> HeaderTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_header, parent, false))
            MetricType.BOOLEAN -> CheckboxTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_checkbox, parent, false))
            MetricType.NUMBER -> CounterTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_counter, parent, false))
            MetricType.STOPWATCH -> StopwatchTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_stopwatch, parent, false))
            MetricType.TEXT -> EditTextTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_notes, parent, false))
            MetricType.LIST -> SpinnerTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_spinner, parent, false))
        }
    }

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        callback.onChildChanged(type, newIndex, oldIndex) {
            super.onChildChanged(type, snapshot, newIndex, oldIndex)
        }
    }
}
