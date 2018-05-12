package com.supercilex.robotscouter.feature.scouts

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.MetricType
import com.supercilex.robotscouter.feature.scouts.viewholder.EditTextViewHolder
import com.supercilex.robotscouter.feature.scouts.viewholder.SpinnerViewHolder
import com.supercilex.robotscouter.shared.scouting.MetricListAdapterBase
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.shared.scouting.viewholder.CheckboxViewHolder
import com.supercilex.robotscouter.shared.scouting.viewholder.CounterViewHolder
import com.supercilex.robotscouter.shared.scouting.viewholder.HeaderViewHolder
import com.supercilex.robotscouter.shared.scouting.viewholder.StopwatchViewHolder

internal class ScoutAdapter(
        metrics: ObservableSnapshotArray<Metric<*>>,
        owner: LifecycleOwner,
        manager: FragmentManager,
        recyclerView: RecyclerView,
        savedInstanceState: Bundle?
) : MetricListAdapterBase(
        FirestoreRecyclerOptions.Builder<Metric<*>>()
                .setSnapshotArray(metrics)
                .setLifecycleOwner(owner)
                .build(),
        recyclerView,
        savedInstanceState,
        manager
) {
    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): MetricViewHolderBase<*, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (MetricType.valueOf(viewType)) {
            MetricType.HEADER -> HeaderViewHolder(
                    inflater.inflate(R.layout.scout_header, parent, false))
            MetricType.BOOLEAN -> CheckboxViewHolder(
                    inflater.inflate(R.layout.scout_checkbox, parent, false))
            MetricType.NUMBER -> CounterViewHolder(
                    inflater.inflate(R.layout.scout_counter, parent, false))
            MetricType.STOPWATCH -> StopwatchViewHolder(
                    inflater.inflate(R.layout.scout_stopwatch, parent, false))
            MetricType.TEXT -> EditTextViewHolder(
                    inflater.inflate(R.layout.scout_notes, parent, false))
            MetricType.LIST -> SpinnerViewHolder(
                    inflater.inflate(R.layout.scout_spinner, parent, false))
        }
    }
}
