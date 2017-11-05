package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.SpinnerViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.longToast

class SpinnerTemplateViewHolder(itemView: View) : SpinnerViewHolder(itemView),
        MetricTemplateViewHolder<Metric.List, Map<String, String>> {
    private val editTitle: String = itemView.context.getString(R.string.metric_spinner_edit_title)

    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    init {
        init()
    }

    override fun updateAdapter() {
        (spinner.adapter as ArrayAdapter<String>).apply {
            clear()
            add(editTitle)
            addAll(metric.value.values)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        if (itemPosition == 0) {
            metric.name = name.text.toString()

            spinner.setSelection(indexOfKey(metric.selectedValueId))
            // TODO Rewrite spinner item stuff
            itemView.context.longToast(
                    "Sorry, updating item selectors hasn't been implemented in the Robot" +
                            " Scouter v2.0 beta yet, but it will be by the time v2.0 reaches" +
                            " the stable channel."
            )
        } else {
            super.onItemSelected(parent, view, itemPosition - 1, id)
        }
    }

    override fun indexOfKey(key: String?) = super.indexOfKey(key) + 1
}
