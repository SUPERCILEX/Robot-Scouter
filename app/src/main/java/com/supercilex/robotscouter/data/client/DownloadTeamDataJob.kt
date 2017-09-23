package com.supercilex.robotscouter.data.client

import android.os.Build
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.startInternetJob14
import com.supercilex.robotscouter.util.data.startInternetJob21

fun startDownloadTeamDataJob(team: Team) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(team, team.number.toInt(), DownloadTeamDataJob21::class.java)
    } else {
        startInternetJob14(team, team.number.toInt(), DownloadTeamDataJob14::class.java)
    }
}
