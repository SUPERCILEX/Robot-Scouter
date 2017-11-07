package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import com.supercilex.robotscouter.data.model.Metric

interface MetricTemplateViewHolder<M : Metric<T>, T> : TemplateViewHolder {
    var metric: M

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        // Only save data when user is leaving metric
        if (!hasFocus && v.id == nameEditor.id) {
            metric.name = nameEditor.text.toString()
        }
    }
}
