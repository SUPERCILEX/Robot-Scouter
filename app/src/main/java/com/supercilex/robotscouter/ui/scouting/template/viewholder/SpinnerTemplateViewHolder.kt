package com.supercilex.robotscouter.ui.scouting.template.viewholder

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scout.viewholder.SpinnerViewHolder
import com.supercilex.robotscouter.ui.scouting.template.SpinnerTemplateDialog
import com.supercilex.robotscouter.util.FIREBASE_VALUE
import java.util.ArrayList
import java.util.LinkedHashMap

class SpinnerTemplateViewHolder(itemView: View) : SpinnerViewHolder(itemView), TemplateViewHolder {
    override fun bind() {
        super.bind()
        name.onFocusChangeListener = this
    }

    override fun getAdapter(listMetric: Metric.List): ArrayAdapter<String> {
        val items = LinkedHashMap<String, String>()
        items.put(metric.ref.push().key, itemView.context.getString(R.string.edit_spinner_items))
        items.putAll(listMetric.value)
        return ArrayAdapter(
                itemView.context, android.R.layout.simple_spinner_item, ArrayList(items.values))
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        if (itemPosition == 0) {
            disableAnimations()
            updateMetricName(name.text.toString())

            SpinnerTemplateDialog.show(
                    manager, metric.ref.child(FIREBASE_VALUE), metric.selectedValueKey)
            spinner.setSelection(indexOfKey(metric.selectedValueKey))
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
