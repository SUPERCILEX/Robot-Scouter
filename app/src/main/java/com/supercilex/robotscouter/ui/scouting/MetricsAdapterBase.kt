package com.supercilex.robotscouter.ui.scouting

import android.arch.lifecycle.LifecycleOwner
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.data.model.HEADER
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.MetricType
import com.supercilex.robotscouter.ui.CardListHelper

abstract class MetricsAdapterBase(metrics: ObservableSnapshotArray<Metric<*>>,
                                  private val manager: FragmentManager,
                                  private val recyclerView: RecyclerView,
                                  owner: LifecycleOwner) :
        FirebaseRecyclerAdapter<Metric<*>, MetricsViewHolderBase<*, *, *>>(
                metrics,
                0,
                MetricsViewHolderBase::class.java,
                owner) {
    protected abstract val cardListHelper: CardListHelper
    private val animator: SimpleItemAnimator = recyclerView.itemAnimator as SimpleItemAnimator

    override fun populateViewHolder(viewHolder: MetricsViewHolderBase<*, *, *>,
                                    metric: Metric<*>,
                                    position: Int) {
        animator.supportsChangeAnimations = true

        cardListHelper.onBind(viewHolder)

        @Suppress("UNCHECKED_CAST")
        viewHolder as MetricsViewHolderBase<Metric<Any>, *, *>
        @Suppress("UNCHECKED_CAST")
        metric as Metric<Any>
        viewHolder.bind(metric, manager, animator)
    }

    @MetricType
    override fun getItemViewType(position: Int): Int = getItem(position).type

    protected inner class ListHelper(hasSafeCorners: Boolean = false) :
            CardListHelper(this, recyclerView, hasSafeCorners) {
        override fun isFirstItem(position: Int): Boolean =
                super.isFirstItem(position) || isHeader(position)

        override fun isLastItem(position: Int): Boolean =
                super.isLastItem(position) || isHeader(position + 1)

        private fun isHeader(position: Int): Boolean = getItem(position).type == HEADER
    }
}
