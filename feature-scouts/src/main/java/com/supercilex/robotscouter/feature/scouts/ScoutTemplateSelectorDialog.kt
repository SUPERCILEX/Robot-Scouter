package com.supercilex.robotscouter.feature.scouts

import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.core.ui.TemplateSelectionListener
import com.supercilex.robotscouter.shared.AddScoutTemplateSelectorDialog

internal class ScoutTemplateSelectorDialog : AddScoutTemplateSelectorDialog() {
    override fun onItemSelected(id: String) {
        super.onItemSelected(id)
        (parentFragment as TemplateSelectionListener).onTemplateSelected(id)
    }

    companion object {
        private const val TAG = "ScoutTemplateSelector"

        fun show(manager: FragmentManager) = ScoutTemplateSelectorDialog().show(manager, TAG)
    }
}
