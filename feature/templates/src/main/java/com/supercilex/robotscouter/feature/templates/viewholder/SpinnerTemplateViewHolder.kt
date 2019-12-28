package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.data.model.updateSelectedValueId
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.core.ui.getDrawableCompat
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.core.ui.showKeyboard
import com.supercilex.robotscouter.core.ui.snackbar
import com.supercilex.robotscouter.core.ui.swap
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.templates.R
import com.supercilex.robotscouter.shared.scouting.MetricListFragment
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.scout_template_base_reorder.*
import kotlinx.android.synthetic.main.scout_template_spinner.*
import kotlinx.android.synthetic.main.scout_template_spinner_item.*
import java.util.Collections
import kotlin.properties.Delegates
import com.supercilex.robotscouter.R as RC

internal class SpinnerTemplateViewHolder(
        itemView: View,
        fragment: MetricListFragment
) : MetricViewHolderBase<Metric.List, List<Metric.List.Item>>(itemView),
        MetricTemplateViewHolder<Metric.List, List<Metric.List.Item>>, View.OnClickListener {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText
    private val itemTouchCallback = ItemTouchCallback()
    private val itemsAdapter = Adapter()

    init {
        init()

        newItem.setOnClickListener(this)

        items.adapter = itemsAdapter
        items.setRecycledViewPool((fragment.parentFragment as RecyclerPoolHolder).recyclerPool)
        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = itemTouchHelper
        itemTouchHelper.attachToRecyclerView(items)
    }

    override fun bind() {
        super.bind()
        itemsAdapter.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        val position = metric.value.size
        metric.update(mutableListOf(
                *getLatestItems().toTypedArray(),
                Metric.List.Item(metric.ref.parent.document().id, "")
        ))
        itemTouchCallback.pendingScrollPosition = position
        itemsAdapter.notifyItemInserted(position)
    }

    private fun getLatestItems(): List<Metric.List.Item> {
        val rv = items
        var items: List<Metric.List.Item> = metric.value
        for (i in 0 until itemsAdapter.itemCount) {
            val holder = rv.findViewHolderForAdapterPosition(i) as ItemHolder?
            items = (holder ?: continue).getUpdatedItems(items)
        }
        return items
    }

    private inner class Adapter : RecyclerView.Adapter<ItemHolder>() {
        override fun getItemCount() = metric.value.size

        override fun getItemViewType(position: Int) = ITEM_TYPE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.scout_template_spinner_item, parent, false)
        )

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val item = itemTouchCallback.getItem(position)
            holder.bind(this@SpinnerTemplateViewHolder, item, metric.selectedValueId == item.id)
            itemTouchCallback.onBind(holder)
        }
    }

    private class ItemHolder(
            override val containerView: View
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer,
            TemplateViewHolder, View.OnClickListener {
        override val reorderView: ImageView by unsafeLazy { reorder }
        override val nameEditor: EditText = itemView.findViewById(RC.id.name)

        private lateinit var parent: SpinnerTemplateViewHolder
        private lateinit var item: Metric.List.Item
        private var isDefault: Boolean by Delegates.notNull()

        init {
            init()
            defaultView.setOnClickListener(this)
            delete.setOnClickListener(this)
            defaultView.setImageDrawable(itemView.context.getDrawableCompat(R.drawable.ic_default_24dp))
        }

        fun bind(parent: SpinnerTemplateViewHolder, item: Metric.List.Item, isDefault: Boolean) {
            this.parent = parent
            this.item = item
            this.isDefault = isDefault

            nameEditor.setText(item.name)
            defaultView.isActivated = isDefault
        }

        override fun onClick(v: View) {
            val items = parent.getLatestItems()
            when (val id = v.id) {
                R.id.defaultView -> updateDefaultStatus(items)
                R.id.delete -> delete(items)
                else -> error("Unknown id: $id")
            }
        }

        private fun updateDefaultStatus(items: List<Metric.List.Item>) {
            if (isDefault) {
                itemView.snackbar(R.string.metric_spinner_item_default_required_error)
                return
            }

            val metric = parent.metric
            val oldDefaultId = metric.selectedValueId
            metric.updateSelectedValueId(item.id)
            parent.items.notifyItemsNoChangeAnimation {
                parent.items.setHasFixedSize(true)
                notifyItemChanged(items.indexOfFirst { it.id == oldDefaultId }.let {
                    if (it == -1) 0 else it
                })
                notifyItemChanged(adapterPosition)
                parent.items.setHasFixedSize(false)
            }
        }

        private fun delete(items: List<Metric.List.Item>) {
            val position = items.indexOfFirst { it.id == item.id }
            if (position == -1) return
            parent.metric.update(items.toMutableList().apply {
                removeAt(position)
            })
            parent.itemsAdapter.notifyItemRemoved(position)

            itemView.longSnackbar(RC.string.deleted, RC.string.undo) {
                parent.metric.update(parent.metric.value.toMutableList().apply {
                    val item = items[position]
                    if (position <= size) add(position, item) else add(item)
                })
                parent.itemsAdapter.notifyItemInserted(position)
            }
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            val metric = parent.metric
            if (
                !hasFocus && v === nameEditor && adapterPosition != -1 &&
                metric.value.any { it.id == item.id }
            ) metric.update(getUpdatedItems(metric.value))
        }

        fun getUpdatedItems(
                value: List<Metric.List.Item>
        ): List<Metric.List.Item> = value.toMutableList().apply {
            this[adapterPosition] = item.copy(name = nameEditor.text.toString()).also {
                item = it
            }
        }
    }

    private inner class ItemTouchCallback : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
    ) {
        var itemTouchHelper: ItemTouchHelper by LateinitVal()
        var pendingScrollPosition: Int = RecyclerView.NO_POSITION
        private var localItems: MutableList<Metric.List.Item>? = null

        fun getItem(position: Int): Metric.List.Item =
                if (localItems == null) metric.value[position] else checkNotNull(localItems)[position]

        fun onBind(viewHolder: ItemHolder) {
            viewHolder.enableDragToReorder(viewHolder, itemTouchHelper)
            if (viewHolder.adapterPosition == pendingScrollPosition) {
                viewHolder.requestFocus()
                viewHolder.nameEditor.let { it.post { it.showKeyboard() } }
                pendingScrollPosition = RecyclerView.NO_POSITION
            }
        }

        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
        ): Boolean {
            var localItems = localItems
            if (localItems == null) {
                // Force a focus on any potential text input to get the most up-to-date data
                nameEditor.requestFocus()
                nameEditor.clearFocus()

                localItems = metric.value.toMutableList()
                this.localItems = localItems
                items.setHasFixedSize(true)
            }

            itemsAdapter.swap(viewHolder, target) { i, j ->
                Collections.swap(localItems, i, j)
            }

            return true
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            items.setHasFixedSize(false)
            localItems?.let {
                metric.update(it)
                localItems = null
            }
        }

        override fun isLongPressDragEnabled() = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int): Unit =
                throw UnsupportedOperationException()
    }

    private companion object {
        const val ITEM_TYPE = 2000
    }
}
