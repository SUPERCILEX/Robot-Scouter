package com.supercilex.robotscouter.data.client

import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.startInternetJob14
import com.supercilex.robotscouter.util.startInternetJob21

fun startDownloadTeamDataJob(context: Context, team: Team) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(
                context,
                team,
                team.numberAsLong.toInt(),
                DownloadTeamDataJob21::class.java)
    } else {
        startInternetJob14(
                context,
                team,
                team.numberAsLong.toInt(),
                DownloadTeamDataJob14::class.java)
    }
}

fun cancelAllDownloadTeamDataJobs(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancelAll()
    } else {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context.applicationContext))
        dispatcher.cancelAll()
    }
}
