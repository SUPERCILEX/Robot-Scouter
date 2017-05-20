package com.supercilex.robotscouter.ui.scout.viewholder

import android.support.annotation.CallSuper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.supercilex.robotscouter.R

open class EditTextViewHolder(itemView: View) :
        ScoutViewHolderBase<String, TextView>(itemView),
        View.OnFocusChangeListener {
    private val mNotes: EditText = itemView.findViewById(R.id.notes) as EditText

    public override fun bind() {
        super.bind()
        mNotes.setText(mMetric.value)
        mNotes.onFocusChangeListener = this
    }

    @CallSuper
    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus && v.id == R.id.notes) updateMetricValue(mNotes.text.toString())
    }
}
