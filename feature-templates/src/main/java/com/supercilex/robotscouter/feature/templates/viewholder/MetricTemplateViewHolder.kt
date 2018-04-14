package com.supercilex.robotscouter.feature.templates.viewholder

import android.view.View
import com.supercilex.robotscouter.core.model.Metric

internal interface MetricTemplateViewHolder<M : Metric<T>, T> : TemplateViewHolder {
    var metric: M

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        // Only save data when user is leaving metric
        if (!hasFocus && v === nameEditor) {
            metric.name = nameEditor.text.toString()
        }
    }
}
