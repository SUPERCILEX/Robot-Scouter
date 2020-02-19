package com.supercilex.robotscouter.feature.scouts.viewholder

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.ui.hideKeyboard
import com.supercilex.robotscouter.feature.scouts.databinding.ScoutNotesBinding
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase

internal class EditTextViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Text, String?>(itemView),
        View.OnFocusChangeListener {
    private val binding = ScoutNotesBinding.bind(itemView)

    init {
        name.onFocusChangeListener = this
    }

    /**
     * Note: this implementation DOES NOT call super
     */
    // We set the text to the value instead of the name because the name goes in the hint
    @SuppressLint("MissingSuperCall")
    public override fun bind() {
        binding.textLayout.isHintAnimationEnabled = false
        name.text = metric.value
        binding.textLayout.hint = metric.name
        binding.textLayout.isHintAnimationEnabled = true

        if (
            Build.VERSION.SDK_INT >= 26 &&
            metric.name.contains(View.AUTOFILL_HINT_NAME, true)
        ) {
            itemView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_AUTO
            name.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            name.setAutofillHints(View.AUTOFILL_HINT_NAME)
        }
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) {
            metric.update(name.text.toString())
            name.hideKeyboard()
        }
    }
}
