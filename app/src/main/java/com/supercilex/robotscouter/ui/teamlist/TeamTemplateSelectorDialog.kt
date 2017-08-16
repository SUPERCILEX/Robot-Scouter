package com.supercilex.robotscouter.ui.teamlist

import android.support.v4.app.FragmentManager
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.getScoutBundle
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.ui.TeamSelectionListener
import com.supercilex.robotscouter.util.ui.TemplateSelectorDialog
import com.supercilex.robotscouter.util.ui.show

class TeamTemplateSelectorDialog : TemplateSelectorDialog() {
    override val title = R.string.title_add_scout_template_selector

    override fun onItemSelected(key: String) {
        (context as TeamSelectionListener).onTeamSelected(getScoutBundle(
                arguments.getTeam(), true, key), false)
    }

    companion object {
        private const val TAG = "TeamTemplateSelector"

        fun show(manager: FragmentManager, team: Team) =
                TeamTemplateSelectorDialog().show(manager, TAG, team.toBundle())
    }
}
