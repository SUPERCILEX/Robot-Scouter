package com.supercilex.robotscouter.util.data.model

import com.supercilex.robotscouter.data.model.Team
import java.util.Collections

open class TeamCache(teams: Collection<Team>) {
    val teams: List<Team> by lazy {
        Collections.unmodifiableList(teams.sorted())
    }
    val teamNames: String by lazy { this.teams.getNames() }
}
