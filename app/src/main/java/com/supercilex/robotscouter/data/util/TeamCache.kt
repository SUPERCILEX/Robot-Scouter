package com.supercilex.robotscouter.data.util

import java.util.*
import kotlin.collections.ArrayList

open class TeamCache(teamHelpers: Collection<TeamHelper>) {
    val teamHelpers: List<TeamHelper> by lazy {
        val sortedTeamHelpers = ArrayList(teamHelpers)
        Collections.sort(sortedTeamHelpers)
        Collections.unmodifiableList(sortedTeamHelpers)
    }
    val teamNames: String by lazy { TeamHelper.getTeamNames(this.teamHelpers) }
}
