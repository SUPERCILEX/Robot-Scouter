package com.supercilex.robotscouter.core.data.remote

import com.supercilex.robotscouter.core.model.Team

internal abstract class TeamServiceBase<out T>(
        team: Team,
        clazz: Class<T>
) : TbaServiceBase<T>(clazz) {
    protected val team: Team = team.copy()

    abstract fun execute(): Team?
}
