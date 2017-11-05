package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CheckboxViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView),
        MetricTemplateViewHolder<Metric.Boolean, Boolean> {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

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
        if (v.id == R.id.checkbox) metric.value = checkbox.isChecked
    }
}
