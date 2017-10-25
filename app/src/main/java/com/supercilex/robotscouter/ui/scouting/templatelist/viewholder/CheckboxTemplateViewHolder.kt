package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CheckboxViewHolder
import kotterknife.bindView

class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView), TemplateViewHolder {
    private val checkBoxName: EditText by bindView(R.id.checkbox_name)

    override fun bind() {
        super.bind()
        name.text = null
        checkBoxName.setText(metric.name)
        checkBoxName.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        if (checkBoxName.hasFocus()) updateMetricName(checkBoxName.text.toString())
        super.onClick(v)
    }

    override fun requestFocus() {
        checkBoxName.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(checkBoxName.text.toString())
    }
}
