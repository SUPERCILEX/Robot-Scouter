package com.supercilex.robotscouter.ui.scout

import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.firebase.database.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.metrics.MetricType
import com.supercilex.robotscouter.ui.CardListHelper
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder
import com.supercilex.robotscouter.ui.scout.viewholder.CounterViewHolder
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder
import com.supercilex.robotscouter.ui.scout.viewholder.HeaderViewHolder
import com.supercilex.robotscouter.ui.scout.viewholder.ScoutViewHolderBase
import com.supercilex.robotscouter.ui.scout.viewholder.SpinnerViewHolder
import com.supercilex.robotscouter.ui.scout.viewholder.StopwatchViewHolder

class ScoutAdapter(query: Query, manager: FragmentManager, recyclerView: RecyclerView) :
        ScoutAdapterBase(query, manager, recyclerView) {
    override val cardListHelper: CardListHelper = ListHelper(true)

    override fun onCreateViewHolder(parent: ViewGroup, @MetricType viewType: Int):
            ScoutViewHolderBase<*, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        when (viewType) {
            MetricType.BOOLEAN -> return CheckboxViewHolder(
                    inflater.inflate(R.layout.scout_checkbox, parent, false))
            MetricType.NUMBER -> return CounterViewHolder(
                    inflater.inflate(R.layout.scout_counter, parent, false))
            MetricType.TEXT -> return EditTextViewHolder(
                    inflater.inflate(R.layout.scout_notes, parent, false))
            MetricType.LIST -> return SpinnerViewHolder(
                    inflater.inflate(R.layout.scout_spinner, parent, false))
            MetricType.STOPWATCH -> return StopwatchViewHolder(
                    inflater.inflate(R.layout.scout_stopwatch, parent, false))
            MetricType.HEADER -> return HeaderViewHolder(
                    inflater.inflate(R.layout.scout_header, parent, false))
            else -> throw IllegalStateException()
        }
    }
}
