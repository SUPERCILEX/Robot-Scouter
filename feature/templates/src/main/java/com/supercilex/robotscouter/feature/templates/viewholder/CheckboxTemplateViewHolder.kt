package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.data.model.updateName
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.viewholder.CheckboxViewHolder
import kotlinx.android.synthetic.main.scout_template_base_reorder.*
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

internal class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Boolean, Boolean> {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText

    private val checkBox = containerView.find<CheckBox>(RC.id.checkBox)

    init {
        init()
        name.setOnClickListener(null)
    }

    override fun onClick(v: View) {
        if (name.hasFocus()) metric.updateName(name.text.toString())
        if (v.id == RC.id.checkBox) metric.update(checkBox.isChecked)
    }
}
