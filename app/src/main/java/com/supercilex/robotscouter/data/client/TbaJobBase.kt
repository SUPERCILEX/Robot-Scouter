package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.supercilex.robotscouter.util.data.parseRawBundle

abstract class TbaJobBase14 : JobService(), TeamJob {
    override fun onStartJob(params: JobParameters): Boolean {
        startJob(parseRawBundle(params.extras!!)).addOnCompleteListener {
            jobFinished(params, !it.isSuccessful)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
abstract class TbaJobBase21 : android.app.job.JobService(), TeamJob {
    override fun onStartJob(params: android.app.job.JobParameters): Boolean {
        startJob(parseRawBundle(params.extras)).addOnCompleteListener {
            jobFinished(params, !it.isSuccessful)
        }
        return true
    }

    override fun onStopJob(params: android.app.job.JobParameters): Boolean = true
}
