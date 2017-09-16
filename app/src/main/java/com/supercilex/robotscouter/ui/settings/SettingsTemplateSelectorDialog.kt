package com.supercilex.robotscouter.ui.settings

import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener
import com.supercilex.robotscouter.util.ui.TemplateSelectorDialog

class SettingsTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.title_pref_default_template

    override fun onItemSelected(id: String) =
            (parentFragment as TemplateSelectionListener).onTemplateSelected(id)

    companion object {
        private const val TAG = "SettingsTemplateSelector"

        fun show(manager: FragmentManager) =
                SettingsTemplateSelectorDialog().show(manager, TAG)
    }
}
