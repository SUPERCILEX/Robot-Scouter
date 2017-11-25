package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

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
) : MetricViewHolderBase<Metric.List, List<Metric.List.Item>, TextView>(itemView),
        AdapterView.OnItemSelectedListener {
    private val spinner: Spinner by bindView(R.id.spinner)

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
        spinner.setSelection(metric.selectedValueId?.let { id ->
            metric.value.indexOfFirst { it.id == id }
        } ?: 0)
    }

    private fun updateAdapter() {
        @Suppress("UNCHECKED_CAST") // We know the metric type
        (spinner.adapter as ArrayAdapter<String>).apply {
            clear()
            addAll(metric.value.map { it.name })
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        metric.selectedValueId = metric.value[itemPosition].id
    }

    override fun onNothingSelected(view: AdapterView<*>) = Unit
}
