package com.supercilex.robotscouter.ui.settings

import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.data.model.TabNamesHolder
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener
import com.supercilex.robotscouter.util.ui.TemplateSelectorDialog

class SettingsTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.title_pref_default_template
    override val holder: TabNamesHolder by lazy {
        ViewModelProviders.of(activity).get(TabNamesHolder::class.java)
    }

    override fun onItemSelected(key: String) =
            (parentFragment as TemplateSelectionListener).onTemplateSelected(key)

    companion object {
        private const val TAG = "SettingsTemplateSelector"

        fun show(manager: FragmentManager) =
                SettingsTemplateSelectorDialog().show(manager, TAG)
    }
}
