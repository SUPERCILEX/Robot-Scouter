package com.supercilex.robotscouter.feature.scouts

import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.shared.TemplateSelectorDialog

internal class ScoutTemplateSelectorDialog : TemplateSelectorDialog() {
    override fun onItemSelected(id: String) {
        super.onItemSelected(id)
        (parentFragment as TemplateSelectionListener).onTemplateSelected(id)
    }

    companion object {
        private const val TAG = "ScoutTemplateSelector"

        fun show(manager: FragmentManager) = ScoutTemplateSelectorDialog().show(manager, TAG)
    }
}
