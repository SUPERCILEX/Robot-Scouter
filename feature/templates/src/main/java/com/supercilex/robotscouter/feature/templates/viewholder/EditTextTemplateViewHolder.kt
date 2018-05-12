package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import kotlinx.android.synthetic.main.scout_template_base_reorder.*
import kotlinx.android.synthetic.main.scout_template_notes.*

internal class EditTextTemplateViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Text, String?>(itemView),
        MetricTemplateViewHolder<Metric.Text, String?> {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText

    init {
        init()
        text.onFocusChangeListener = this
    }

    override fun bind() {
        super.bind()
        text.setText(metric.value)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        super.onFocusChange(v, hasFocus)
        if (!hasFocus && v === text) metric.update(text.text.toString())
    }
}
