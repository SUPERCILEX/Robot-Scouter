package com.supercilex.robotscouter.data.util

import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.getTeamNames
import java.util.Collections

open class TeamCache(teams: Collection<Team>) {
    val teams: List<Team> by lazy {
        val sortedTeams = ArrayList(teams)
        Collections.sort(sortedTeams)
        Collections.unmodifiableList(sortedTeams)
    }
    val teamNames: String by lazy { getTeamNames(this.teams) }
}
