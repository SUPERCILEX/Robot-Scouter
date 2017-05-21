package com.supercilex.robotscouter.data.client

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.support.annotation.RequiresApi
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.JobUtils

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
abstract class TbaJobBase21 : JobService() {
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
