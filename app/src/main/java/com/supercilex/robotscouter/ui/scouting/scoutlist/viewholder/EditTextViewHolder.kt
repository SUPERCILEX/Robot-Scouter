package com.supercilex.robotscouter.ui.scouting.scoutlist.viewholder

import android.annotation.SuppressLint
import android.os.Build
import android.support.annotation.CallSuper
import android.support.design.widget.TextInputLayout
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.ui.scouting.MetricViewHolderBase
import kotterknife.bindView

open class EditTextViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Text, String?, TextView>(itemView),
        View.OnFocusChangeListener {
    private val textLayout: TextInputLayout by bindView(R.id.text_layout)

    /**
     * Note: this implementation DOES NOT call super
     */
    // We set the text to the value instead of the name because the name goes in the hint
    @SuppressLint("MissingSuperCall")
    public override fun bind() {
        textLayout.isHintAnimationEnabled = false
        name.text = metric.value
        textLayout.hint = metric.name
        textLayout.isHintAnimationEnabled = true

        name.onFocusChangeListener = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && metric.name.toUpperCase().contains(View.AUTOFILL_HINT_NAME.toUpperCase())) {
            itemView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_AUTO
            name.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            name.setAutofillHints(View.AUTOFILL_HINT_NAME)
        }
    }

    @CallSuper
    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) metric.value = name.text.toString()
    }
}
