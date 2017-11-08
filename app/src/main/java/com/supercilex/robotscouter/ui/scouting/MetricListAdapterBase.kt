package com.supercilex.robotscouter.ui.scouting

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.MetricType
import com.supercilex.robotscouter.util.ui.CardListHelper
import com.supercilex.robotscouter.util.ui.SavedStateAdapter

@Suppress("UNCHECKED_CAST") // Needed to support extension
abstract class MetricListAdapterBase(
        options: FirestoreRecyclerOptions<Metric<*>>,
        recyclerView: RecyclerView,
        savedInstanceState: Bundle?,
        private val manager: FragmentManager
) : SavedStateAdapter<Metric<*>, MetricViewHolderBase<*, *, *>>(
        options,
        savedInstanceState,
        recyclerView
) {
    private val cardListHelper: CardListHelper = object : CardListHelper(this, recyclerView) {
        override fun isFirstItem(position: Int): Boolean =
                super.isFirstItem(position) || isHeader(position)

        override fun isLastItem(position: Int): Boolean =
                super.isLastItem(position) || isHeader(position + 1)

        private fun isHeader(position: Int): Boolean = getItem(position).type == MetricType.HEADER
    }

    override fun onBindViewHolder(
            viewHolder: MetricViewHolderBase<*, *, *>,
            position: Int,
            metric: Metric<*>
    ) {
        cardListHelper.onBind(viewHolder)

        viewHolder as MetricViewHolderBase<Metric<Any>, *, *>
        metric as Metric<Any>
        viewHolder.bind(metric, manager)
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type.id

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type == ChangeEventType.CHANGED) {
            // Check to see if this change comes from the device or the server. All ViewHolder have
            // a contract to update the local copies of their backing Metric. Thus, we can find the
            // ViewHolder at the specified position and see if the metric we get from the server is
            // identical to the one on-device. If so, no-op.

            val metric = snapshots[newIndex]
            recyclerView.findViewHolderForAdapterPosition(newIndex)?.let {
                val holder = it as MetricViewHolderBase<Metric<Any>, *, *>
                if (holder.metric == metric && holder.metric.position == metric.position) {
                    return
                }
            }
        }

        super.onChildChanged(type, snapshot, newIndex, oldIndex)
        cardListHelper.onChildChanged(type, newIndex)
    }
}
