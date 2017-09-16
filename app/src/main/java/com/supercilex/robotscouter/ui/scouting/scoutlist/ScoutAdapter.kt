package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.arch.lifecycle.LifecycleOwner
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.BOOLEAN
import com.supercilex.robotscouter.data.model.HEADER
import com.supercilex.robotscouter.data.model.LIST
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.MetricType
import com.supercilex.robotscouter.data.model.NUMBER
import com.supercilex.robotscouter.data.model.STOPWATCH
import com.supercilex.robotscouter.data.model.TEXT
import com.supercilex.robotscouter.ui.scouting.MetricListAdapterBase
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CheckboxViewHolder
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CounterViewHolder
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.EditTextViewHolder
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.HeaderViewHolder
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.SpinnerViewHolder
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.StopwatchViewHolder

class ScoutAdapter(metrics: ObservableSnapshotArray<Metric<*>>,
                   manager: FragmentManager,
                   recyclerView: RecyclerView,
                   owner: LifecycleOwner) :
        MetricListAdapterBase(FirestoreRecyclerOptions.Builder<Metric<*>>()
                                      .setSnapshotArray(metrics)
                                      .setLifecycleOwner(owner)
                                      .build(),
                              manager,
                              recyclerView) {
    override fun onCreateViewHolder(parent: ViewGroup, @MetricType viewType: Int):
            MetricViewHolderBase<*, *, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            HEADER -> HeaderViewHolder(
                    inflater.inflate(R.layout.scout_header, parent, false))
            BOOLEAN -> CheckboxViewHolder(
                    inflater.inflate(R.layout.scout_checkbox, parent, false))
            NUMBER -> CounterViewHolder(
                    inflater.inflate(R.layout.scout_counter, parent, false))
            STOPWATCH -> StopwatchViewHolder(
                    inflater.inflate(R.layout.scout_stopwatch, parent, false))
            TEXT -> EditTextViewHolder(
                    inflater.inflate(R.layout.scout_notes, parent, false))
            LIST -> SpinnerViewHolder(
                    inflater.inflate(R.layout.scout_spinner, parent, false))
            else -> throw IllegalStateException("Unknown view type: $viewType")
        }
    }
}
