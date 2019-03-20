package com.supercilex.robotscouter.core.data.remote

import com.supercilex.robotscouter.core.model.Team

internal abstract class TeamServiceBase<out T>(
        protected val team: Team,
        clazz: Class<T>
) : TbaServiceBase<T>(clazz) {
    abstract suspend fun execute(): Team?
}
