package com.supercilex.robotscouter.ui.scout

import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.Query
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.metrics.MetricType
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric
import com.supercilex.robotscouter.data.util.ScoutUtils
import com.supercilex.robotscouter.ui.CardListHelper
import com.supercilex.robotscouter.ui.scout.viewholder.*

open class ScoutAdapter(
        query: Query, private val mManager: FragmentManager, recyclerView: RecyclerView) :
        FirebaseRecyclerAdapter<ScoutMetric<Any>, ScoutViewHolderBase<*, *>>(
                ScoutUtils.METRIC_PARSER,
                0,
                ScoutViewHolderBase::class.java,
                query) {
    private val mAnimator: SimpleItemAnimator = recyclerView.itemAnimator as SimpleItemAnimator
    private val mCardListHelper: CardListHelper

    init {
        mCardListHelper = object : CardListHelper(this, recyclerView, true) {
            override fun isFirstItem(position: Int): Boolean =
                    super.isFirstItem(position) || isHeader(position)

            override fun isLastItem(position: Int): Boolean =
                    super.isLastItem(position) || isHeader(position + 1)

            private fun isHeader(position: Int): Boolean = getItem(position).type == MetricType.HEADER
        }
    }

    public override fun populateViewHolder(
            viewHolder: ScoutViewHolderBase<*, *>,
            metric: ScoutMetric<Any>,
            position: Int) {
        mAnimator.supportsChangeAnimations = true

        mCardListHelper.onBind(viewHolder)

        @Suppress("UNCHECKED_CAST")
        viewHolder as ScoutViewHolderBase<Any, *>
        viewHolder.bind(metric, mManager, mAnimator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, @MetricType viewType: Int):
            ScoutViewHolderBase<*, *> {
        val inflater = LayoutInflater.from(parent.context)
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

    @MetricType
    override fun getItemViewType(position: Int): Int = getItem(position).type
}
