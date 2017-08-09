package com.supercilex.robotscouter.data.client

import android.content.Context
import android.os.Build
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.startInternetJob14
import com.supercilex.robotscouter.util.data.startInternetJob21

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
