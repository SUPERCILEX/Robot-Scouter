package com.supercilex.robotscouter.shared.scouting.viewholder

import android.os.Build
import android.view.ContextMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.supercilex.robotscouter.common.second
import com.supercilex.robotscouter.core.data.model.add
import com.supercilex.robotscouter.core.data.model.remove
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.shared.scouting.R
import kotlinx.android.synthetic.main.scout_base_stopwatch.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeout
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import com.supercilex.robotscouter.core.ui.R as RC

open class StopwatchViewHolder(
        itemView: View,
        fragment: MetricListFragment
) : MetricViewHolderBase<Metric.Stopwatch, List<Long>>(itemView),
        View.OnClickListener, View.OnLongClickListener {
    private var timer: Timer? = null
    private var undoAddSnackbar: Snackbar? = null

    private val onToOffIcon by unsafeLazy {
        checkNotNull(AnimatedVectorDrawableCompat.create(
                itemView.context, R.drawable.ic_timer_on_to_off_24dp))
    }
    private val offToOnIcon by unsafeLazy {
        checkNotNull(AnimatedVectorDrawableCompat.create(
                itemView.context, R.drawable.ic_timer_off_to_on_24dp))
    }

    private val cyclesAdapter = Adapter()

    init {
        stopwatch.setOnClickListener(this)
        stopwatch.setOnLongClickListenerCompat(this)

        cycles.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
        ).apply {
            initialPrefetchItemCount = 6
        }
        cycles.adapter = cyclesAdapter
        GravitySnapHelper(Gravity.START).attachToRecyclerView(cycles)

        cycles.setRecycledViewPool((fragment.parentFragment as RecyclerPoolHolder).recyclerPool)
    }

    override fun bind() {
        super.bind()
        cycles.setHasFixedSize(false)
        cyclesAdapter.notifyDataSetChanged()

        val timer = TIMERS[metric]
        if (timer == null) {
            setText(R.string.metric_stopwatch_start_title)
            updateStyle(false, false)
        } else {
            timer.holder = this
            updateStyle(true, false)
            timer.updateButtonTime()
        }
        this.timer = timer
    }

    override fun onClick(v: View) {
        val currentTimer = timer
        if (currentTimer == null) {
            undoAddSnackbar?.dismiss()
            timer = Timer(this)
        } else {
            val lap = currentTimer.cancel()
            metric.add(metric.value.size, lap)

            metric.value.size.let { notifyCycleAdded(it, it) }

            itemView.longSnackbar(R.string.scout_stopwatch_lap_added_message, RC.string.undo) {
                val hadAverage = metric.value.size >= LIST_SIZE_WITH_AVERAGE
                metric.remove(lap)

                notifyCycleRemoved(metric.value.size, metric.value.size, hadAverage)
                timer = Timer(this, currentTimer.startTimeMillis)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        val currentTimer = timer ?: return false

        currentTimer.cancel()
        undoAddSnackbar = itemView.longSnackbar(RC.string.cancelled, RC.string.undo) {
            timer = Timer(this, currentTimer.startTimeMillis)
        }.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                undoAddSnackbar = null
            }
        })

        return true
    }

    private fun notifyCycleAdded(position: Int, size: Int) {
        // Force RV to request layout when adding first item
        cycles.setHasFixedSize(size >= LIST_SIZE_WITH_AVERAGE)

        if (size == LIST_SIZE_WITH_AVERAGE) {
            updateFirstCycleName()
            cyclesAdapter.notifyItemInserted(0) // Add the average card
            cyclesAdapter.notifyItemInserted(position)
        } else {
            // Account for the average card being there or not. Since we are adding a new lap,
            // there are only two possible states: 1 item or n + 2 items.
            cyclesAdapter.notifyItemInserted(if (size == 1) 0 else position)
            // Ensure the average card is updated if it's there
            cyclesAdapter.notifyItemChanged(0)

            updateCycleNames(position, size)
        }
    }

    private fun notifyCycleRemoved(position: Int, size: Int, hadAverage: Boolean) {
        // Force RV to request layout when removing last item
        cycles.setHasFixedSize(size > 0)

        cyclesAdapter.notifyItemRemoved(if (hadAverage) position + 1 else position)
        if (hadAverage && size == 1) {
            cyclesAdapter.notifyItemRemoved(0) // Remove the average card
            updateFirstCycleName()
        } else if (size >= LIST_SIZE_WITH_AVERAGE) {
            cyclesAdapter.notifyItemChanged(0) // Ensure the average card is updated
            updateCycleNames(position, size)
        }
    }

    private fun updateFirstCycleName() =
            cycles.notifyItemsNoChangeAnimation { notifyItemChanged(0) }

    private fun updateCycleNames(position: Int, size: Int) {
        if (size >= LIST_SIZE_WITH_AVERAGE) {
            cycles.notifyItemsNoChangeAnimation {
                notifyItemRangeChanged(position + 1, size - position)
            }
        }
    }

    private fun setText(@StringRes id: Int, vararg formatArgs: Any) {
        stopwatch.text = itemView.resources.getString(id, *formatArgs)
    }

    private fun updateStyle(isRunning: Boolean, animate: Boolean = true) {
        // There's a bug pre-L where changing the view state doesn't update the vector drawable.
        // Because of that, calling View#setActivated(isRunning) doesn't update the background
        // color and we end up with unreadable text.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        stopwatch.isActivated = isRunning

        if (animate) {
            val drawable = if (isRunning) onToOffIcon else offToOnIcon

            stopwatch.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            drawable.start()
        } else {
            stopwatch.setCompoundDrawablesRelativeWithIntrinsicBounds(if (isRunning) {
                R.drawable.ic_timer_off_24dp
            } else {
                R.drawable.ic_timer_on_24dp
            }, 0, 0, 0)
        }
    }

    private class Timer(
            holder: StopwatchViewHolder,
            val startTimeMillis: Long = System.currentTimeMillis()
    ) {
        private val updater: Job

        /**
         * The ID of the metric who originally started the request. We need to validate it since
         * ViewHolders may be recycled across different instances.
         */
        private val metric = holder.metric

        private var _holder: WeakReference<StopwatchViewHolder> = WeakReference(holder)
        var holder: StopwatchViewHolder?
            get() = _holder.get()?.takeIf { it.metric.ref == metric.ref }
            set(holder) {
                _holder = WeakReference<StopwatchViewHolder>(holder)
            }

        /** @return the time since this class was instantiated in milliseconds */
        private val elapsedTimeMillis: Long
            get() = System.currentTimeMillis() - startTimeMillis

        init {
            TIMERS[holder.metric] = this

            updater = GlobalScope.launch(Dispatchers.Main) {
                try {
                    withTimeout(GAME_TIME_MINS, TimeUnit.MINUTES) {
                        while (isActive) {
                            updateButtonTime()
                            delay(1, TimeUnit.SECONDS)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    cancel()
                }
            }

            updateStyle()
            updateButtonTime()
        }

        fun updateButtonTime() {
            setText(R.string.metric_stopwatch_stop_title, getFormattedTime(elapsedTimeMillis))
        }

        /** @return the time since this class was instantiated and then cancelled */
        fun cancel(): Long {
            holder?.timer = null
            TIMERS.filter { it.value == this }.forEach {
                TIMERS.remove(it.key)
            }

            updater.cancel()
            updateStyle()
            setText(R.string.metric_stopwatch_start_title)

            return elapsedTimeMillis
        }

        private fun setText(@StringRes id: Int, vararg formatArgs: Any) {
            holder?.setText(id, *formatArgs)
        }

        private fun updateStyle() {
            val holder = holder ?: return
            TransitionManager.beginDelayedTransition(holder.itemView as ViewGroup, transition)
            holder.updateStyle(updater.isActive)
        }

        private companion object {
            const val GAME_TIME_MINS = 3L

            val transition = AutoTransition().apply {
                excludeTarget(RecyclerView::class.java, true)
            }
        }
    }

    private inner class Adapter : RecyclerView.Adapter<DataHolder>() {
        val hasAverageItems get() = metric.value.size > 1

        override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
        ): DataHolder = LayoutInflater.from(parent.context).inflate(
                R.layout.scout_stopwatch_cycle_item,
                parent,
                false
        ).apply {
            layoutParams = layoutParams.apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }.let {
            if (viewType == DATA_ITEM) CycleHolder(it) else AverageHolder(it)
        }

        override fun onBindViewHolder(holder: DataHolder, position: Int) {
            val cycles = metric.value
            when {
                holder is AverageHolder -> holder.bind(cycles)
                hasAverageItems -> {
                    val realIndex = position - 1
                    holder.bind(this@StopwatchViewHolder, cycles[realIndex])
                }
                else -> holder.bind(this@StopwatchViewHolder, cycles[position])
            }
        }

        override fun getItemCount(): Int {
            val size = metric.value.size
            return if (hasAverageItems) size + 1 else size
        }

        override fun getItemViewType(position: Int): Int =
                if (hasAverageItems && position == 0) AVERAGE_ITEM else DATA_ITEM
    }

    private abstract class DataHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        protected val title: TextView = itemView.find(android.R.id.text1)
        protected val value: TextView = itemView.find(android.R.id.text2)

        /**
         * The outclass's instance. Used indirectly since this ViewHolder may be recycled across
         * different instances.
         */
        private lateinit var holder: StopwatchViewHolder

        private val hasAverage get() = holder.cyclesAdapter.hasAverageItems
        protected val realPosition
            get() = if (hasAverage) adapterPosition - 1 else adapterPosition

        @CallSuper
        open fun bind(holder: StopwatchViewHolder, nanoTime: Long) {
            this.holder = holder
        }

        override fun onCreateContextMenu(
                menu: ContextMenu,
                v: View,
                menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu.add(R.string.delete).setOnMenuItemClickListener(this)
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            val metric = holder.metric
            val hadAverage = hasAverage
            val position = realPosition
            val rawPosition = adapterPosition

            val newCycles = metric.value.toMutableList()
            val deletedCycle = newCycles.removeAt(position)
            metric.remove(deletedCycle)

            holder.notifyCycleRemoved(position, metric.value.size, hadAverage)

            itemView.longSnackbar(R.string.deleted, R.string.undo) {
                val latestMetric = holder.metric
                latestMetric.add(position, deletedCycle)
                holder.notifyCycleAdded(rawPosition, latestMetric.value.size)
            }

            return true
        }
    }

    private class CycleHolder(itemView: View) : DataHolder(itemView) {
        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun bind(holder: StopwatchViewHolder, nanoTime: Long) {
            super.bind(holder, nanoTime)
            title.text = itemView.context.getString(
                    R.string.metric_stopwatch_cycle_title, realPosition + 1)
            value.text = getFormattedTime(nanoTime)
        }
    }

    private class AverageHolder(itemView: View) : DataHolder(itemView) {
        init {
            title.setText(R.string.metric_stopwatch_cycle_average_title)
        }

        fun bind(cycles: List<Long>) {
            value.text = getFormattedTime(cycles.sum() / cycles.size)
        }
    }

    private companion object {
        val TIMERS = ConcurrentHashMap<Metric.Stopwatch, Timer>()

        const val LIST_SIZE_WITH_AVERAGE = 2
        // Don't conflict with metric types since the pool is shared
        const val DATA_ITEM = 1000
        const val AVERAGE_ITEM = 1001

        private const val COLON = ":"
        private const val LEADING_ZERO = "0"

        fun getFormattedTime(nanos: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(nanos)
            return (minutes.toString() + COLON +
                    (TimeUnit.MILLISECONDS.toSeconds(nanos) - minutes * 60)).let {
                val split = it.split(COLON.toRegex()).dropLastWhile {
                    it.isEmpty()
                }.toTypedArray()
                if (split.second().length > 1) {
                    it
                } else {
                    split.first() + COLON + LEADING_ZERO + split.second()
                }
            }
        }
    }
}
