package com.supercilex.robotscouter.ui.scouting.templatelist

import android.arch.lifecycle.LifecycleOwner
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.database.DataSnapshot
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
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.CheckboxTemplateViewHolder
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.CounterTemplateViewHolder
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.EditTextTemplateViewHolder
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.HeaderTemplateViewHolder
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.SpinnerTemplateViewHolder
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.StopwatchTemplateViewHolder
import com.supercilex.robotscouter.util.ui.CardListHelper

class TemplateAdapter(metrics: ObservableSnapshotArray<Metric<*>>,
                      manager: FragmentManager,
                      recyclerView: RecyclerView,
                      owner: LifecycleOwner,
                      private val callback: TemplateItemTouchCallback<Metric<*>>) :
        MetricListAdapterBase(metrics, manager, recyclerView, owner) {
    override val cardListHelper: CardListHelper = ListHelper()

    override fun getItem(position: Int) = callback.getItem(position)

    override fun getRef(position: Int) = callback.getRef(position)

    override fun populateViewHolder(viewHolder: MetricViewHolderBase<*, *, *>,
                                    metric: Metric<*>,
                                    position: Int) {
        super.populateViewHolder(viewHolder, metric, position)
        callback.onBind(viewHolder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, @MetricType viewType: Int):
            MetricViewHolderBase<*, *, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            HEADER -> HeaderTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_header, parent, false))
            BOOLEAN -> CheckboxTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_checkbox, parent, false))
            NUMBER -> CounterTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_counter, parent, false))
            STOPWATCH -> StopwatchTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_stopwatch, parent, false))
            TEXT -> EditTextTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_notes, parent, false))
            LIST -> SpinnerTemplateViewHolder(
                    inflater.inflate(R.layout.scout_template_spinner, parent, false))
            else -> throw IllegalStateException("Unknown view type: $viewType")
        }
    }

    override fun onChildChanged(type: ChangeEventListener.EventType,
                                snapshot: DataSnapshot,
                                index: Int,
                                oldIndex: Int) {
        if (callback.onChildChanged(type, index)) {
            super.onChildChanged(type, snapshot, index, oldIndex)
        }
    }
}
