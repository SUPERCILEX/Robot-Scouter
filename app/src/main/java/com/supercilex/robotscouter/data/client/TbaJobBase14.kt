package com.supercilex.robotscouter.data.client

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.parseRawBundle

abstract class TbaJobBase14 : JobService() {
    protected abstract val updateTeam: (team: Team, newTeam: Team) -> Unit

    override fun onStartJob(params: JobParameters): Boolean {
        val team: Team = parseRawBundle(params.extras!!)
        startTask(team).addOnSuccessListener { newTeam ->
            updateTeam(team, newTeam)
            jobFinished(params, false)
        }.addOnFailureListener { jobFinished(params, true) }
        return true
    }

    abstract fun startTask(previousTeam: Team): Task<Team>

    override fun onStopJob(params: JobParameters?): Boolean = true
}
