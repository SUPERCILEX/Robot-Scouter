package com.supercilex.robotscouter.feature.scouts.viewholder

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.supercilex.robotscouter.core.data.model.updateSelectedValueId
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.feature.scouts.databinding.ScoutSpinnerBinding
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase

internal class SpinnerViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.List, List<Metric.List.Item>>(itemView),
        AdapterView.OnItemSelectedListener {
    private val binding = ScoutSpinnerBinding.bind(itemView)

    init {
        binding.spinner.adapter = ArrayAdapter<String>(
                itemView.context,
                android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinner.setBackgroundResource(
                com.google.android.material.R.drawable.abc_spinner_mtrl_am_alpha)
        binding.spinner.onItemSelectedListener = this
    }

    public override fun bind() {
        super.bind()
        updateAdapter()
        binding.spinner.setSelection(metric.selectedValueId?.let { id ->
            metric.value.indexOfFirst { it.id == id }
        } ?: 0)
    }

    private fun updateAdapter() {
        @Suppress("UNCHECKED_CAST") // We know the metric type
        (binding.spinner.adapter as ArrayAdapter<String>).apply {
            clear()
            addAll(metric.value.map { it.name })
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, itemPosition: Int, id: Long) {
        metric.updateSelectedValueId(metric.value[itemPosition].id)
    }

    override fun onNothingSelected(view: AdapterView<*>) = Unit
}
