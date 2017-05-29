package com.supercilex.robotscouter.ui.scout

import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.firebase.database.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.BOOLEAN
import com.supercilex.robotscouter.data.model.HEADER
import com.supercilex.robotscouter.data.model.LIST
import com.supercilex.robotscouter.data.model.MetricType
import com.supercilex.robotscouter.data.model.NUMBER
import com.supercilex.robotscouter.data.model.STOPWATCH
import com.supercilex.robotscouter.data.model.TEXT
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
            ScoutViewHolderBase<*, *, *> {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        when (viewType) {
            BOOLEAN -> return CheckboxViewHolder(
                    inflater.inflate(R.layout.scout_checkbox, parent, false))
            NUMBER -> return CounterViewHolder(
                    inflater.inflate(R.layout.scout_counter, parent, false))
            TEXT -> return EditTextViewHolder(
                    inflater.inflate(R.layout.scout_notes, parent, false))
            LIST -> return SpinnerViewHolder(
                    inflater.inflate(R.layout.scout_spinner, parent, false))
            STOPWATCH -> return StopwatchViewHolder(
                    inflater.inflate(R.layout.scout_stopwatch, parent, false))
            HEADER -> return HeaderViewHolder(
                    inflater.inflate(R.layout.scout_header, parent, false))
            else -> throw IllegalStateException()
        }
    }
}
