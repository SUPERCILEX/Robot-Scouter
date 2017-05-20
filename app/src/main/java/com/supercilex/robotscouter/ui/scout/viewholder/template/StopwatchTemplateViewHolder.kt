package com.supercilex.robotscouter.ui.scout.viewholder.template

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.view.View

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scout.viewholder.StopwatchViewHolder

class StopwatchTemplateViewHolder(itemView: View) : StopwatchViewHolder(itemView), ScoutTemplateViewHolder {
    override fun bind() {
        super.bind()
        mName.onFocusChangeListener = this

        val layout = itemView as ConstraintLayout
        val set = ConstraintSet()
        set.clone(layout)
        set.connect(R.id.list, ConstraintSet.LEFT, R.id.reorder, ConstraintSet.RIGHT, 0)
        set.applyTo(layout)
    }

    override fun requestFocus() {
        mName.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(mName.text.toString())
    }
}
