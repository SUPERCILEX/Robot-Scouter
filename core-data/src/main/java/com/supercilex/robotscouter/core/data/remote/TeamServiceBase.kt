package com.supercilex.robotscouter.core.data.remote

import com.supercilex.robotscouter.core.model.Team
import java.util.Calendar

internal abstract class TeamServiceBase<out T>(
        team: Team,
        clazz: Class<T>
) : TbaServiceBase<T>(clazz) {
    protected val team: Team = team.copy()
    protected val year: Int get() = Calendar.getInstance().get(Calendar.YEAR)

    abstract fun execute(): Team?
}
