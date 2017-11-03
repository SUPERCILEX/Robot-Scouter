package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.CheckboxViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView), TemplateViewHolder {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    override fun bind() {
        super.bind()
        name.setOnClickListener(null)
        name.text = metric.name
        name.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        if (name.hasFocus()) updateMetricName(name.text.toString())
        if (v.id == R.id.checkbox) updateMetricValue(checkbox.isChecked)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(name.text.toString())
    }
}
