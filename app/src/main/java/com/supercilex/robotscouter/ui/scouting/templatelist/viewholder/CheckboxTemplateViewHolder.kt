package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CheckboxViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.android.synthetic.main.scout_base_checkbox.*
import kotlinx.android.synthetic.main.scout_template_base_reorder.*

class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Boolean, Boolean> {
    override val reorderView: ImageView by unsafeLazy { reorder }
    override val nameEditor = name as EditText

    init {
        init()
        name.setOnClickListener(null)
    }

    override fun bind() {
        super.bind()
        name.text = metric.name
    }

    override fun onClick(v: View) {
        if (name.hasFocus()) metric.name = name.text.toString()
        if (v.id == R.id.checkBox) metric.value = checkBox.isChecked
    }
}
