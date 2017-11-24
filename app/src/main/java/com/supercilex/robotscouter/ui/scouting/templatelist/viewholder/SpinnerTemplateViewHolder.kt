package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.app.Activity
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.ui.notifyItemsNoChangeAnimation
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.find
import kotlin.properties.Delegates

class SpinnerTemplateViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.List, List<Metric.List.Item>, TextView>(itemView),
        MetricTemplateViewHolder<Metric.List, List<Metric.List.Item>>, View.OnClickListener {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    private val newItem: ImageButton by bindView(R.id.new_item)
    private val items: RecyclerView by bindView(R.id.list)

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
    }

    override fun bind() {
        super.bind()
        items.adapter.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        metric.value = metric.value.toMutableList().apply {
            add(Metric.List.Item(metric.ref.parent.document().id, "", size))
        }
        items.adapter.notifyItemInserted(metric.value.size - 1)
    }

    private inner class Adapter : RecyclerView.Adapter<ItemHolder>() {
        override fun getItemCount() = metric.value.size

        override fun getItemViewType(position: Int) = ITEM_TYPE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.scout_template_spinner_item, parent, false)
        )

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val item = metric.value[position]
            holder.bind(this@SpinnerTemplateViewHolder, item, metric.selectedValueId == item.id)
        }
    }

    private class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TemplateViewHolder,
            View.OnClickListener {
        override val reorder: View by bindView(R.id.reorder)
        override val nameEditor: EditText by bindView(R.id.name)
        private val star: ImageButton by bindView(R.id.star)

        private lateinit var parent: SpinnerTemplateViewHolder
        private lateinit var item: Metric.List.Item
        private var isDefault: Boolean by Delegates.notNull()

        init {
            init()
            star.setOnClickListener(this)
        }

        fun bind(parent: SpinnerTemplateViewHolder, item: Metric.List.Item, isDefault: Boolean) {
            this.parent = parent
            this.item = item
            this.isDefault = isDefault

            nameEditor.setText(item.name)
            star.setImageResource(if (isDefault) {
                R.drawable.ic_star_accent_24dp
            } else {
                R.drawable.ic_star_outline_accent_24dp
            })
        }

        override fun onClick(v: View?) {
            if (isDefault) {
                snackbar(
                        (itemView.context as Activity).find(R.id.root),
                        R.string.metric_spinner_item_default_required_error
                )
                return
            }

            val oldId = parent.metric.selectedValueId
            parent.metric.selectedValueId = item.id
            parent.items.notifyItemsNoChangeAnimation {
                notifyItemChanged(parent.metric.value.indexOfFirst { it.id == oldId }.let {
                    if (it == -1) 0 else it
                })
                notifyItemChanged(adapterPosition)
            }
        }

        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (hasFocus || v.id != nameEditor.id) return

            val metric = parent.metric
            metric.value = metric.value.toMutableList().apply {
                this[metric.value.indexOf(item)] = item.copy(name = nameEditor.text.toString()).also {
                    item = it
                }
            }
        }
    }

    private companion object {
        const val ITEM_TYPE = 2000
    }
}
