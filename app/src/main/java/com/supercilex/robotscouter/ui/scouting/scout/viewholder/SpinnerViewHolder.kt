package com.supercilex.robotscouter.ui.scouting.scout.viewholder

import android.support.annotation.CallSuper
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import java.util.ArrayList

open class SpinnerViewHolder(itemView: View) :
        MetricViewHolderBase<Metric.List, Map<String, String>, TextView>(itemView), AdapterView.OnItemSelectedListener {
    protected var spinner: Spinner = itemView.findViewById(R.id.spinner)
    private val keys: List<String> get() = ArrayList(metric.value.keys)

    public override fun bind() {
        super.bind()
        if (metric.value.isEmpty()) return

        val spinnerArrayAdapter = getAdapter(metric)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = spinnerArrayAdapter
        spinner.onItemSelectedListener = this
        spinner.setSelection(indexOfKey(metric.selectedValueKey))
    }

    @CallSuper
    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        if (indexOfKey(metric.selectedValueKey) != itemPosition) {
            disableAnimations()
            metric.selectedValueKey = keys[itemPosition]
        }
    }

    protected open fun indexOfKey(key: String?): Int {
        return keys.indices.firstOrNull { TextUtils.equals(key, keys[it]) } ?: 0
    }

    protected open fun getAdapter(listMetric: Metric.List): ArrayAdapter<String> {
        return ArrayAdapter(itemView.context,
                android.R.layout.simple_spinner_item,
                ArrayList(listMetric.value.values))
    }

    override fun onNothingSelected(view: AdapterView<*>) {
        // Cancel
    }
}
