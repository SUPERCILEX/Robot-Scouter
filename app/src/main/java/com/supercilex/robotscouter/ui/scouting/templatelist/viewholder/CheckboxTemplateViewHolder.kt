package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CheckboxViewHolder
import kotterknife.bindView

class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView), TemplateViewHolder {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by bindView(R.id.checkbox_name)

    override fun bind() {
        super.bind()
        name.text = null
        nameEditor.setText(metric.name)
        nameEditor.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        if (nameEditor.hasFocus()) updateMetricName(nameEditor.text.toString())
        super.onClick(v)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(nameEditor.text.toString())
    }
}
