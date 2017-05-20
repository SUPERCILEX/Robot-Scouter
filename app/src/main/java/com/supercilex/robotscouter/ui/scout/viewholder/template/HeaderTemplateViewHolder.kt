package com.supercilex.robotscouter.ui.scout.viewholder.template

import android.view.View

import com.supercilex.robotscouter.ui.scout.viewholder.HeaderViewHolder

class HeaderTemplateViewHolder(itemView: View) : HeaderViewHolder(itemView), ScoutTemplateViewHolder {
    override fun bind() {
        super.bind()
        mName.onFocusChangeListener = this
    }

    override fun requestFocus() {
        mName.requestFocus()
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(mName.text.toString())
    }
}
