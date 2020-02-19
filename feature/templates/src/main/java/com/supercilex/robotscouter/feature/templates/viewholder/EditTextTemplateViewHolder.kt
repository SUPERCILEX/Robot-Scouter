package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.templates.R
import com.supercilex.robotscouter.feature.templates.databinding.ScoutTemplateNotesBinding
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase

internal class EditTextTemplateViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Text, String?>(itemView),
        MetricTemplateViewHolder<Metric.Text, String?> {
    private val binding = ScoutTemplateNotesBinding.bind(itemView)

    override val reorderView: ImageView by unsafeLazy { itemView.findViewById(R.id.reorder) }
    override val nameEditor = name as EditText

    init {
        init()
        binding.text.onFocusChangeListener = this
    }

    override fun bind() {
        super.bind()
        binding.text.setText(metric.value)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        super.onFocusChange(v, hasFocus)
        if (!hasFocus && v === binding.text) metric.update(binding.text.text.toString())
    }
}
