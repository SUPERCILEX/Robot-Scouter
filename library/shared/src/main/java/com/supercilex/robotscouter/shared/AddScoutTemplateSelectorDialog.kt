package com.supercilex.robotscouter.shared

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.widget.AppCompatCheckBox
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.unsafeLazy

abstract class AddScoutTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.add_scout_selector_title
    private val setAsDefaultCheckbox: CheckBox by unsafeLazy {
        AppCompatCheckBox(context).apply {
            @SuppressLint("PrivateResource", "ResourceType") // I'm lazy
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                val checkboxMarginCompensation =
                        resources.getDimensionPixelSize(R.dimen.spacing_checkbox_margin_hack)

                context.withStyledAttributes(null, intArrayOf(
                        R.attr.listPreferredItemPaddingLeft,
                        R.attr.listPreferredItemPaddingRight
                )) {
                    leftMargin = getDimensionPixelSize(0, 0) + checkboxMarginCompensation
                    rightMargin = getDimensionPixelSize(1, 0) + checkboxMarginCompensation
                }
            }
            setText(R.string.template_set_default_title)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as LinearLayout).addView(setAsDefaultCheckbox)
    }

    @CallSuper
    override fun onItemSelected(id: String) {
        if (setAsDefaultCheckbox.isChecked) defaultTemplateId = id
    }
}
