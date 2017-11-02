package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.SpinnerViewHolder
import org.jetbrains.anko.longToast
import java.util.ArrayList
import java.util.LinkedHashMap

class SpinnerTemplateViewHolder(itemView: View) : SpinnerViewHolder(itemView), TemplateViewHolder {
    override fun bind() {
        super.bind()
        name.onFocusChangeListener = this
    }

    override fun getAdapter(listMetric: Metric.List): ArrayAdapter<String> {
        val items = LinkedHashMap<String, String>()
        items.put(
                metric.ref.parent.document().id,
                itemView.context.getString(R.string.metric_spinner_edit_title)
        )
        items.putAll(listMetric.value)
        return ArrayAdapter(
                itemView.context, android.R.layout.simple_spinner_item, ArrayList(items.values))
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        if (itemPosition == 0) {
            disableAnimations()
            updateMetricName(name.text.toString())

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

    override fun requestFocus() {
        name.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(name.text.toString())
    }
}
