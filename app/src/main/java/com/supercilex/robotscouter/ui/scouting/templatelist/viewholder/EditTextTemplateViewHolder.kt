package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class EditTextTemplateViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Text, String?, TextView>(itemView), TemplateViewHolder {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }
    private val text: EditText by bindView(R.id.text)

    override fun bind() {
        super.bind()
        text.setText(metric.value)
        name.onFocusChangeListener = this
        text.onFocusChangeListener = this
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus && v.id == R.id.name) updateMetricName(name.text.toString())
        if (!hasFocus && v.id == R.id.text) updateMetricValue(text.text.toString())
    }
}
