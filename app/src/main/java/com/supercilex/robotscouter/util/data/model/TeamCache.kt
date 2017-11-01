package com.supercilex.robotscouter.util.data.model

import com.supercilex.robotscouter.data.model.Team
import java.util.Collections

open class TeamCache(teams: Collection<Team>) {
    val teams: List<Team> by lazy {
        val sortedTeams = ArrayList(teams)
        Collections.sort(sortedTeams)
        Collections.unmodifiableList(sortedTeams)
    }
    val teamNames: String by lazy { this.teams.getNames() }
}
