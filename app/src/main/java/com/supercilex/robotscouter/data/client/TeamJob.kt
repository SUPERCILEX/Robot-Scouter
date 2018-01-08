package com.supercilex.robotscouter.data.client

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor

interface TeamJob {
    val updateTeam: (team: Team, newTeam: Team) -> Unit

    fun startJob(
            team: Team
    ): Task<*> = startTask(team).continueWith(AsyncTaskExecutor, Continuation<Team, Unit> {
        updateTeam(team, it.result)
    })

    fun startTask(previousTeam: Team): Task<Team>
}
