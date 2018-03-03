package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.os.Build
import android.support.annotation.StringRes
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ContextMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.util.reportOrCancel
import com.supercilex.robotscouter.util.second
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.setOnLongClickListenerCompat
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import org.jetbrains.anko.runOnUiThread
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

open class StopwatchViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Stopwatch, List<Long>, TextView>(itemView),
        View.OnClickListener, View.OnLongClickListener {
    private val toggleStopwatch: Button by bindView(R.id.stopwatch)
    private val cycles: RecyclerView by bindView(R.id.list)

    private var timer: Timer? = null
    private var undoAddSnackbar: Snackbar? = null

    init {
        toggleStopwatch.setOnClickListener(this)
        toggleStopwatch.setOnLongClickListenerCompat(this)

        cycles.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
        ).apply {
            initialPrefetchItemCount = 6
        }
        cycles.adapter = Adapter()
        GravitySnapHelper(Gravity.START).attachToRecyclerView(cycles)

        (itemView.context as FragmentActivity).supportFragmentManager.fragments
                .filterIsInstance<RecyclerPoolHolder>()
                .single()
                .let { cycles.recycledViewPool = it.recyclerPool }
    }

    override fun bind() {
        super.bind()
        cycles.setHasFixedSize(false)
        cycles.adapter.notifyDataSetChanged()

        val timer = TIMERS[metric]
        if (timer == null) {
            setText(R.string.metric_stopwatch_start_title)
            updateStyle(false)
        } else {
            timer.holder = this
            updateStyle(true)
            timer.updateButtonTime()
            this.timer = timer
        }
    }

    override fun onClick(v: View) {
        val currentTimer = timer
        if (currentTimer == null) {
            undoAddSnackbar?.dismiss()
            timer = Timer(this)
        } else {
            metric.value = metric.value.toMutableList().apply {
                add(currentTimer.cancel())
            }

            val size = metric.value.size
            notifyCycleAdded(size, size)

            longSnackbar(itemView, R.string.scout_stopwatch_lap_added_message, R.string.undo) {
                val hadAverage = metric.value.size >= LIST_SIZE_WITH_AVERAGE
                val newCycles = metric.value.toMutableList().apply {
                    removeAt(size - 1)
                }
                metric.value = newCycles

                notifyCycleRemoved(metric.value.size, metric.value.size, hadAverage)
                timer = Timer(this, currentTimer.startTimeMillis)
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        val currentTimer = timer ?: return false

        currentTimer.cancel()
        undoAddSnackbar = longSnackbar(itemView, R.string.cancelled, R.string.undo) {
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

        val adapter = cycles.adapter
        if (size == LIST_SIZE_WITH_AVERAGE) {
            // Add the average card
            adapter.notifyItemInserted(0)
            adapter.notifyItemInserted(position)
        } else {
            // Account for the average card being there or not. Since we are adding a new lap,
            // there are only two possible states: 1 item or n + 2 items.
            adapter.notifyItemInserted(if (size == 1) 0 else position)
            // Ensure the average card is updated if it's there
            adapter.notifyItemChanged(0)
        }
    }

    private fun notifyCycleRemoved(position: Int, size: Int, hadAverage: Boolean) {
        // Force RV to request layout when removing last item
        cycles.setHasFixedSize(size > 0)

        val adapter = cycles.adapter
        adapter.notifyItemRemoved(if (hadAverage) position + 1 else position)
        if (hadAverage && size == 1) {
            // Remove the average card
            adapter.notifyItemRemoved(0)
        } else if (size >= LIST_SIZE_WITH_AVERAGE) {
            adapter.notifyItemChanged(0) // Ensure the average card is updated
        }
    }

    private fun setText(@StringRes id: Int, vararg formatArgs: Any) {
        toggleStopwatch.text = itemView.resources.getString(id, *formatArgs)
    }

    private fun updateStyle(isRunning: Boolean) {
        // There's a bug pre-L where changing the view state doesn't update the vector drawable.
        // Because of that, calling View#setActivated(isRunning) doesn't update the background
        // color and we end up with unreadable text.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toggleStopwatch.isActivated = isRunning
        }
    }

    private class Timer(
            holder: StopwatchViewHolder,
            val startTimeMillis: Long = System.currentTimeMillis()
    ) : Runnable {
        private val updater: Future<*>

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
        private val elapsedTime: Long
            get() = System.currentTimeMillis() - startTimeMillis

        init {
            TIMERS[holder.metric] = this
            updater = executor.scheduleWithFixedDelay(this, 1, 1, TimeUnit.SECONDS)
            updateStyle()
            updateButtonTime()
        }

        override fun run() {
            if (TimeUnit.SECONDS.toMinutes(TimeUnit.MILLISECONDS.toSeconds(elapsedTime)) >= GAME_TIME) {
                cancel()
                return
            }

            RobotScouter.runOnUiThread { updateButtonTime() }
        }

        fun updateButtonTime() {
            setText(R.string.metric_stopwatch_stop_title, getFormattedTime(elapsedTime))
        }

        /** @return the time since this class was instantiated and then cancelled */
        fun cancel(): Long {
            holder?.timer = null
            TIMERS.filter { it.value == this }.forEach {
                TIMERS.remove(it.key)
            }

            updater.reportOrCancel(true)
            RobotScouter.runOnUiThread {
                updateStyle()
                setText(R.string.metric_stopwatch_start_title)
            }

            return elapsedTime
        }

        private fun setText(@StringRes id: Int, vararg formatArgs: Any) {
            holder?.setText(id, *formatArgs)
        }

        private fun updateStyle() {
            val holder = holder ?: return
            TransitionManager.beginDelayedTransition(holder.itemView as ViewGroup, transition)
            holder.updateStyle(!updater.isDone)
        }

        private companion object {
            const val GAME_TIME = 3

            val executor = ScheduledThreadPoolExecutor(1)
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
            if (viewType == DATA_ITEM) DataHolder(it) else AverageHolder(it)
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

    private open class DataHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        protected val title: TextView = itemView.find(android.R.id.text1)
        protected val value: TextView = itemView.find(android.R.id.text2)

        /**
         * The outclass's instance. Used indirectly since this ViewHolder may be recycled across
         * different instances.
         */
        private lateinit var holder: StopwatchViewHolder

        private val hasAverage
            get() = (holder.cycles.adapter as Adapter).hasAverageItems
        private val realPosition
            get() = if (hasAverage) adapterPosition - 1 else adapterPosition

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        fun bind(holder: StopwatchViewHolder, nanoTime: Long) {
            this.holder = holder

            title.text = itemView.context.getString(
                    R.string.metric_stopwatch_cycle_title, realPosition + 1)
            value.text = getFormattedTime(nanoTime)
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
            metric.value = newCycles

            holder.notifyCycleRemoved(position, metric.value.size, hadAverage)

            longSnackbar(itemView, R.string.deleted, R.string.undo) {
                val latestMetric = holder.metric

                latestMetric.value = latestMetric.value.toMutableList().apply {
                    add(position, deletedCycle)
                }

                holder.notifyCycleAdded(rawPosition, latestMetric.value.size)
            }

            return true
        }
    }

    private class AverageHolder(itemView: View) : DataHolder(itemView) {
        init {
            title.setText(R.string.metric_stopwatch_cycle_average_title)
            itemView.setOnCreateContextMenuListener(null)
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
