package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.widget.AppCompatCheckBox
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.ui.TemplateSelectorDialog

abstract class AddScoutTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.title_add_scout_template_selector
    private val setAsDefaultCheckbox: CheckBox by lazy {
        AppCompatCheckBox(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                val checkboxMarginCompensation =
                        resources.getDimension(R.dimen.spacing_checkbox_margin_hack).toInt()

                val a = context.obtainStyledAttributes(intArrayOf(
                        R.attr.listPreferredItemPaddingLeft, R.attr.listPreferredItemPaddingRight))
                leftMargin = a.getDimensionPixelSize(0, 0) + checkboxMarginCompensation
                rightMargin = a.getDimensionPixelSize(1, 0) + checkboxMarginCompensation
                a.recycle()
            }
            setText(R.string.title_set_default_template)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootView.addView(setAsDefaultCheckbox)
    }

    @CallSuper
    override fun onItemSelected(key: String) {
        if (setAsDefaultCheckbox.isChecked) defaultTemplateKey = key
    }
}
