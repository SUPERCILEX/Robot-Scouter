package com.supercilex.robotscouter.data.client

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.JobUtils

abstract class TbaJobBase14 : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        startTask(JobUtils.parseRawBundle(params.extras).team).addOnSuccessListener { newTeam ->
            JobUtils.parseRawBundle(params.extras).updateMedia(newTeam)
            jobFinished(params, false)
        }.addOnFailureListener { jobFinished(params, true) }
        return true
    }

    abstract fun startTask(previousTeam: Team): Task<Team>

    override fun onStopJob(params: JobParameters?): Boolean = true
}
