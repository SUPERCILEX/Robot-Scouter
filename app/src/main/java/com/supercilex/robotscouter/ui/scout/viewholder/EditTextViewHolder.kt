package com.supercilex.robotscouter.ui.scout.viewholder

import android.support.annotation.CallSuper
import android.support.design.widget.TextInputLayout
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric

open class EditTextViewHolder(itemView: View) :
        ScoutViewHolderBase<Metric.Text, String?, TextView>(itemView),
        View.OnFocusChangeListener {
    private val textLayout: TextInputLayout = itemView.findViewById(R.id.text_layout)

    public override fun bind() {
        super.bind()
        name.text = metric.value
        textLayout.hint = metric.name
        name.onFocusChangeListener = this
    }

    @CallSuper
    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) updateMetricValue(name.text.toString())
    }
}
