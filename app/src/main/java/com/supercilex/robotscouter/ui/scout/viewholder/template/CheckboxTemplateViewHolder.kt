package com.supercilex.robotscouter.ui.scout.viewholder.template

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.view.View
import android.widget.EditText

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scout.viewholder.CheckboxViewHolder

class CheckboxTemplateViewHolder(itemView: View) : CheckboxViewHolder(itemView), ScoutTemplateViewHolder {
    private val mCheckBoxName: EditText = itemView.findViewById(R.id.checkbox_name) as EditText

    init {
        updateConstraints(itemView as ConstraintLayout)
    }

    override fun bind() {
        super.bind()
        mName.text = null
        mCheckBoxName.setText(mMetric.name)
        mCheckBoxName.onFocusChangeListener = this
    }

    override fun onClick(v: View) {
        if (mCheckBoxName.hasFocus()) updateMetricName(mCheckBoxName.text.toString())
        super.onClick(v)
    }

    override fun requestFocus() {
        mCheckBoxName.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(mCheckBoxName.text.toString())
    }

    private fun updateConstraints(layout: ConstraintLayout) {
        val set = ConstraintSet()
        set.clone(layout)
        set.connect(R.id.name, ConstraintSet.LEFT, R.id.reorder, ConstraintSet.RIGHT, 0)
        set.applyTo(layout)
    }
}
