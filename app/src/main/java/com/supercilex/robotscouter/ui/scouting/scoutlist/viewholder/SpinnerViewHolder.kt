package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import kotterknife.bindView

class SpinnerViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.List, Map<String, String>, TextView>(itemView),
        AdapterView.OnItemSelectedListener {
    private val spinner: Spinner by bindView(R.id.spinner)
    private val ids: Set<String>
        get() = metric.value.keys

    init {
        spinner.adapter = ArrayAdapter<String>(
                itemView.context,
                android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.onItemSelectedListener = this
    }

    public override fun bind() {
        super.bind()
        updateAdapter()
        spinner.setSelection(indexOfKey(metric.selectedValueId))
    }

    private fun updateAdapter() {
        @Suppress("UNCHECKED_CAST") // We know the metric type
        (spinner.adapter as ArrayAdapter<String>).apply {
            clear()
            addAll(metric.value.values)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        metric.selectedValueId = ids.elementAt(itemPosition)
    }

    private fun indexOfKey(key: String?): Int = ids.forEachIndexed { index, test ->
        if (TextUtils.equals(test, key)) return index
    }.run {
        return 0
    }

    override fun onNothingSelected(view: AdapterView<*>) = Unit
}
