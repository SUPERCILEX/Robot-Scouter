package com.supercilex.robotscouter.feature.teams

import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.core.data.getScoutBundle
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.show
import com.supercilex.robotscouter.shared.AddScoutTemplateSelectorDialog

internal class TeamTemplateSelectorDialog : AddScoutTemplateSelectorDialog() {
    override fun onItemSelected(id: String) {
        super.onItemSelected(id)
        (context as TeamSelectionListener)
                .onTeamSelected(getScoutBundle(checkNotNull(arguments).getTeam(), true, id), false)
    }

    companion object {
        private const val TAG = "TeamTemplateSelector"

        fun show(manager: FragmentManager, team: Team) =
                TeamTemplateSelectorDialog().show(manager, TAG, team.toBundle())
    }
}
