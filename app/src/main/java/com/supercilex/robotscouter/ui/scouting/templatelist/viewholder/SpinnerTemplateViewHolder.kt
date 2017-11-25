package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.arch.core.executor.ArchTaskExecutor
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.ui.scouting.templatelist.TemplateAdapter
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.util.ui.showKeyboard
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import java.util.Collections
import kotlin.properties.Delegates

class SpinnerTemplateViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.List, List<Metric.List.Item>, TextView>(itemView),
        MetricTemplateViewHolder<Metric.List, List<Metric.List.Item>>, View.OnClickListener {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    private val newItem: ImageButton by bindView(R.id.new_item)
    private val items: RecyclerView by bindView(R.id.list)
    private val itemTouchCallback = ItemTouchCallback()

    init {
        init()

        newItem.setOnClickListener(this)

        items.layoutManager = LinearLayoutManager(itemView.context)
        items.adapter = Adapter()
        for (fragment in (itemView.context as FragmentActivity).supportFragmentManager.fragments) {
            if (fragment is RecyclerPoolHolder) {
                items.recycledViewPool = (fragment as RecyclerPoolHolder).recyclerPool
                break
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = itemTouchHelper
        itemTouchHelper.attachToRecyclerView(items)
    }

    override fun bind() {
        super.bind()
        items.adapter.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        val position = metric.value.size
        metric.value = metric.value.toMutableList().apply {
            add(Metric.List.Item(metric.ref.parent.document().id, ""))
        }
        itemTouchCallback.pendingScrollPosition = position
        items.adapter.notifyItemInserted(position)
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

    private class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TemplateViewHolder,
            View.OnClickListener {
        override val reorder: View by bindView(R.id.reorder)
        override val nameEditor: EditText by bindView(R.id.name)
        private val default: ImageButton by bindView(R.id.default_)
        private val delete: ImageButton by bindView(R.id.delete)

        private lateinit var parent: SpinnerTemplateViewHolder
        private lateinit var item: Metric.List.Item
        private var isDefault: Boolean by Delegates.notNull()

        init {
            init()
            default.setOnClickListener(this)
            delete.setOnClickListener(this)
        }

        fun bind(parent: SpinnerTemplateViewHolder, item: Metric.List.Item, isDefault: Boolean) {
            this.parent = parent
            this.item = item
            this.isDefault = isDefault

            nameEditor.setText(item.name)
            default.setImageResource(if (isDefault) {
                R.drawable.ic_star_accent_24dp
            } else {
                R.drawable.ic_star_outline_accent_24dp
            })
        }

        override fun onClick(v: View) {
            var view: View? = itemView
            do {
                if (view is RecyclerView && view.adapter is TemplateAdapter) {
                    view.clearFocus() // Save data
                    break
                }

                if (view != null) {
                    view = view.parent as? View?
                }
            } while (view != null)

            when (v.id) {
                R.id.default_ -> updateDefaultStatus()
                R.id.delete -> delete()
                else -> throw IllegalStateException("Unknown id: ${v.id}")
            }
        }

        private fun updateDefaultStatus() {
            if (isDefault) {
                snackbar(itemView, R.string.metric_spinner_item_default_required_error)
                return
            }

            val oldId = parent.metric.selectedValueId
            parent.metric.selectedValueId = item.id
            parent.items.notifyItemsNoChangeAnimation {
                parent.items.setHasFixedSize(true)
                notifyItemChanged(parent.metric.value.indexOfFirst { it.id == oldId }.let {
                    if (it == -1) 0 else it
                })
                notifyItemChanged(adapterPosition)
                parent.items.setHasFixedSize(false)
            }
        }

        private fun delete() {
            val oldItems = parent.metric.value
            val position = adapterPosition
            parent.metric.value = oldItems.toMutableList().apply {
                removeAt(position)
            }
            parent.items.adapter.notifyItemRemoved(position)

            longSnackbar(itemView, R.string.deleted, R.string.undo) {
                parent.metric.value = oldItems
                parent.items.adapter.notifyItemInserted(position)
            }
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (hasFocus || v.id != nameEditor.id) return

            val metric = parent.metric
            metric.value = metric.value.toMutableList().apply {
                this[metric.value.indexOfFirst { it.id == item.id }] = item.copy(
                        name = nameEditor.text.toString()
                ).also {
                    item = it
                }
            }
        }
    }

    private inner class ItemTouchCallback : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
    ) {
        var itemTouchHelper: ItemTouchHelper by LateinitVal()
        var pendingScrollPosition: Int = RecyclerView.NO_POSITION
        private var localItems: List<Metric.List.Item>? = null

        fun getItem(position: Int): Metric.List.Item =
                if (localItems == null) metric.value[position] else localItems!![position]

        fun onBind(viewHolder: RecyclerView.ViewHolder) {
            viewHolder as TemplateViewHolder

            viewHolder.enableDragToReorder(viewHolder, itemTouchHelper)
            if (viewHolder.adapterPosition == pendingScrollPosition) {
                viewHolder.requestFocus()
                ArchTaskExecutor.getInstance().postToMainThread {
                    viewHolder.nameEditor.showKeyboard()
                }
                pendingScrollPosition = RecyclerView.NO_POSITION
            }
        }

        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
        ): Boolean {
            if (localItems == null) {
                localItems = metric.value.toMutableList()
                items.setHasFixedSize(true)
            }

            val fromPos = viewHolder.adapterPosition
            val toPos = target.adapterPosition

            if (fromPos < toPos) {
                for (i in fromPos until toPos) swapDown(i)
            } else {
                for (i in fromPos downTo toPos + 1) swapUp(i)
            }
            items.adapter.notifyItemMoved(fromPos, toPos)

            return true
        }

        private fun swapDown(i: Int) = swap(i, i + 1)

        private fun swapUp(i: Int) = swap(i, i - 1)

        private fun swap(i: Int, j: Int) {
            Collections.swap(localItems, i, j)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            items.setHasFixedSize(false)
            localItems?.let {
                metric.value = it
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
