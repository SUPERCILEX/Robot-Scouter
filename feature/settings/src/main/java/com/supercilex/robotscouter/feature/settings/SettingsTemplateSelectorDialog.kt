package com.supercilex.robotscouter.feature.settings

import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.core.ui.TemplateSelectionListener
import com.supercilex.robotscouter.shared.TemplateSelectorDialog

internal class SettingsTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.settings_pref_default_template_title

    override fun onItemSelected(id: String) =
            (parentFragment as TemplateSelectionListener).onTemplateSelected(id)

    companion object {
        private const val TAG = "SettingsTemplateSelector"

        fun show(manager: FragmentManager) = SettingsTemplateSelectorDialog().show(manager, TAG)
    }
}
