package com.supercilex.robotscouter.feature.scouts.viewholder

import android.annotation.SuppressLint
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.View
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.unsafeLazy
import com.supercilex.robotscouter.shared.scouting.MetricViewHolderBase
import kotlinx.android.synthetic.main.scout_notes.*
import java.util.Locale

internal class EditTextViewHolder(
        itemView: View
) : MetricViewHolderBase<Metric.Text, String?>(itemView),
        View.OnFocusChangeListener {
    init {
        name.onFocusChangeListener = this
    }

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

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            metric.name.toUpperCase(Locale.ROOT).contains(nameHint)
        ) {
            itemView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_AUTO
            name.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            name.setAutofillHints(View.AUTOFILL_HINT_NAME)
        }
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (!hasFocus) metric.update(name.text.toString())
    }

    private companion object {
        @get:RequiresApi(Build.VERSION_CODES.O)
        val nameHint by unsafeLazy { View.AUTOFILL_HINT_NAME.toUpperCase() }
    }
}
