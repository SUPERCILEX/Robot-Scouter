package com.supercilex.robotscouter.ui.scouting

import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.data.model.HEADER
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.MetricType
import com.supercilex.robotscouter.util.ui.CardListHelper

abstract class MetricListAdapterBase(options: FirestoreRecyclerOptions<Metric<*>>,
                                     private val manager: FragmentManager,
                                     private val recyclerView: RecyclerView) :
        FirestoreRecyclerAdapter<Metric<*>, MetricViewHolderBase<*, *, *>>(options) {
    private val cardListHelper: CardListHelper = object : CardListHelper(this, recyclerView) {
        override fun isFirstItem(position: Int): Boolean =
                super.isFirstItem(position) || isHeader(position)

        override fun isLastItem(position: Int): Boolean =
                super.isLastItem(position) || isHeader(position + 1)

        private fun isHeader(position: Int): Boolean = getItem(position).type == HEADER
    }
    private val animator: SimpleItemAnimator = recyclerView.itemAnimator as SimpleItemAnimator

    override fun onBindViewHolder(viewHolder: MetricViewHolderBase<*, *, *>,
                                  position: Int,
                                  metric: Metric<*>) {
        animator.supportsChangeAnimations = true

        cardListHelper.onBind(viewHolder)

        @Suppress("UNCHECKED_CAST")
        viewHolder as MetricViewHolderBase<Metric<Any>, *, *>
        @Suppress("UNCHECKED_CAST")
        metric as Metric<Any>
        viewHolder.bind(metric, manager, animator)
    }

    @MetricType
    override fun getItemViewType(position: Int): Int = getItem(position).type

    override fun onChildChanged(type: ChangeEventType,
                                snapshot: DocumentSnapshot,
                                newIndex: Int,
                                oldIndex: Int) {
        super.onChildChanged(type, snapshot, newIndex, oldIndex)
        cardListHelper.onChildChanged(type, newIndex)
    }
}
