package com.supercilex.robotscouter.data.client

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.util.data.parseTeam
import com.supercilex.robotscouter.util.logFailures
import kotlinx.coroutines.experimental.async

abstract class TbaJobBase14 : com.firebase.jobdispatcher.JobService(), TeamJob {
    override fun onStartJob(params: com.firebase.jobdispatcher.JobParameters): Boolean {
        async {
            val failed = try {
                startJob(params.extras!!.parseTeam())
            } catch (e: Exception) {
                e
            } is Exception
            jobFinished(params, failed)
        }.logFailures()
        return true
    }

    override fun onStopJob(params: com.firebase.jobdispatcher.JobParameters): Boolean = true
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
abstract class TbaJobBase21 : JobService(), TeamJob {
    override fun onStartJob(params: JobParameters): Boolean {
        async {
            val failed = try {
                startJob(params.extras.parseTeam())
            } catch (e: Exception) {
                e
            } is Exception
            jobFinished(params, failed)
        }.logFailures()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}
