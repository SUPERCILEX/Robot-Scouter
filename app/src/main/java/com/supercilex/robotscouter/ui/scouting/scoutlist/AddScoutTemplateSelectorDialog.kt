package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.widget.AppCompatCheckBox
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.content.withStyledAttributes
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.ui.TemplateSelectorDialog
import com.supercilex.robotscouter.util.unsafeLazy

abstract class AddScoutTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.template_add_scout_selector_title
    private val setAsDefaultCheckbox: CheckBox by unsafeLazy {
        AppCompatCheckBox(context).apply {
            @SuppressLint("PrivateResource", "ResourceType") // I'm lazy
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                val checkboxMarginCompensation =
                        resources.getDimension(R.dimen.spacing_checkbox_margin_hack).toInt()

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
