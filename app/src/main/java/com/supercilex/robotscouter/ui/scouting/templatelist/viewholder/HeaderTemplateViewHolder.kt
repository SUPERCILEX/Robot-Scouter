package com.supercilex.robotscouter.ui.scouting.templatelist.viewholder

import android.view.View
import android.widget.EditText
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder.HeaderViewHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView

class HeaderTemplateViewHolder(itemView: View) : HeaderViewHolder(itemView), TemplateViewHolder {
    override val reorder: View by bindView(R.id.reorder)
    override val nameEditor: EditText by unsafeLazy { name as EditText }

    override fun bind() {
        super.bind()
        name.onFocusChangeListener = this
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricName(name.text.toString())
    }
}
