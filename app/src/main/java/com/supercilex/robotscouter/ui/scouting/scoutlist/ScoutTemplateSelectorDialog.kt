package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.TemplateSelectionListener

class ScoutTemplateSelectorDialog : AddScoutTemplateSelectorDialog() {
    override val title = R.string.template_add_scout_selector_title

    override fun onItemSelected(id: String) {
        super.onItemSelected(id)
        (parentFragment as TemplateSelectionListener).onTemplateSelected(id)
    }

    companion object {
        private const val TAG = "ScoutTemplateSelector"

        fun show(manager: FragmentManager) =
                ScoutTemplateSelectorDialog().show(manager, TAG)
    }
}
