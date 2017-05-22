package com.supercilex.robotscouter.ui.scout.viewholder

import android.support.annotation.CallSuper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.supercilex.robotscouter.R

open class EditTextViewHolder(itemView: View) :
        ScoutViewHolderBase<String, TextView>(itemView),
        View.OnFocusChangeListener {
    private val notes: EditText = itemView.findViewById(R.id.notes)

    public override fun bind() {
        super.bind()
        notes.setText(metric.value)
        notes.onFocusChangeListener = this
    }

    @CallSuper
    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus && v.id == R.id.notes) updateMetricValue(notes.text.toString())
    }
}
