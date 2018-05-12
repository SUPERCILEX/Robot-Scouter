package com.supercilex.robotscouter.feature.teams

import com.supercilex.robotscouter.core.model.Team

interface SelectedTeamsRetriever {
    val selectedTeams: List<Team>
}

interface TeamExporter {
    fun export()
}

interface DrawerToggler {
    fun toggle(enabled: Boolean)
}

interface SignInResolver {
    fun showSignInResolution()
}
