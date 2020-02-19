package com.supercilex.robotscouter.feature.templates.viewholder

import android.os.Build
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import com.supercilex.robotscouter.core.data.model.updateName
import com.supercilex.robotscouter.core.data.model.updateUnit
import com.supercilex.robotscouter.core.data.nullOrFull
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.feature.templates.R
import com.supercilex.robotscouter.feature.templates.databinding.ScoutTemplateCounterBinding
import com.supercilex.robotscouter.shared.scouting.viewholder.CounterViewHolder
import com.supercilex.robotscouter.R as RC

internal class CounterTemplateViewHolder(itemView: View) : CounterViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Number, Long> {
    private val binding = ScoutTemplateCounterBinding.bind(itemView)

    override val reorderView: ImageView by unsafeLazy { itemView.findViewById(R.id.reorder) }
    override val nameEditor = name as EditText

    init {
        init()

        itemView as LinearLayout
        itemView.removeView(binding.unit)
        itemView.addView(binding.unit, itemView.childCount - 1)
        itemView.findViewById<View>(RC.id.count_container).apply {
            if (Build.VERSION.SDK_INT >= 17) {
                updatePaddingRelative(end = 0)
            } else {
                updatePadding(right = 0)
            }
        }

        binding.unit.onFocusChangeListener = this
    }

    override fun bind() {
        super.bind()
        binding.unit.setText(metric.unit)
    }

    override fun onClick(v: View) {
        super.onClick(v)
        if (name.hasFocus()) metric.updateName(name.text.toString())
    }

    override fun bindValue(count: TextView) {
        count.text = countFormat.format(metric.value)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        super.onFocusChange(v, hasFocus)
        if (!hasFocus && v === binding.unit) {
            metric.updateUnit(binding.unit.text.nullOrFull()?.toString())
        }
    }
}
