package com.supercilex.robotscouter.ui.scout.viewholder.template

import android.view.View

import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scout.viewholder.EditTextViewHolder

class EditTextTemplateViewHolder(itemView: View) : EditTextViewHolder(itemView), ScoutTemplateViewHolder {
    override fun bind() {
        super.bind()
        mName.onFocusChangeListener = this
    }

    override fun requestFocus() {
        mName.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        super.onFocusChange(v, hasFocus)
        if (!hasFocus && v.id == R.id.name) updateMetricName(mName.text.toString())
    }
}
