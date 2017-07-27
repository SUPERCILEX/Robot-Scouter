package com.supercilex.robotscouter.ui.scouting.template.viewholder

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.view.View

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scout.viewholder.StopwatchViewHolder

class StopwatchTemplateViewHolder(itemView: View) : StopwatchViewHolder(itemView), TemplateViewHolder {
    override fun bind() {
        super.bind()
        name.onFocusChangeListener = this

        itemView as ConstraintLayout
        val set = ConstraintSet()
        set.clone(itemView)
        set.connect(R.id.list, ConstraintSet.START, R.id.reorder, ConstraintSet.END, 0)
        set.applyTo(itemView)
    }

    override fun requestFocus() {
        name.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(name.text.toString())
    }
}
