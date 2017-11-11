package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class SpinnerTemplateViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.List, Map<String, String>, TextView>(itemView),
        MetricTemplateViewHolder<Metric.List, Map<String, String>>, View.OnClickListener {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    private val newItem: Button by bindView(R.id.new_item)
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

    override fun onClick(v: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private inner class Adapter : RecyclerView.Adapter<ItemHolder>() {
        override fun getItemCount() = metric.value.size

        override fun getItemViewType(position: Int) = ITEM_TYPE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.scout_template_spinner_item, parent, false))

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val id = metric.value.keys.elementAt(position)
            holder.bind(metric.value[id]!!, metric.selectedValueId == id)
        }
    }

    private class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: EditText by bindView(R.id.name)
        private val star: ImageButton by bindView(R.id.star)

        init {

        }

        fun bind(name: String, isDefault: Boolean) {
            this.name.text = name
            star.setImageResource(if (isDefault) {
                R.drawable.ic_star_accent_24dp
            } else {
                R.drawable.ic_star_outline_accent_24dp
            })
        }
    }

    private companion object {
        const val ITEM_TYPE = 2000
    }
}
