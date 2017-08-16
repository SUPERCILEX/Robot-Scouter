package com.supercilex.robotscouter.data.client

import android.os.Build
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.startInternetJob14
import com.supercilex.robotscouter.util.data.startInternetJob21

fun startUploadTeamMediaJob(team: Team) {
    val mediaHash: Int = team.media!!.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(team, mediaHash, UploadTeamMediaJob21::class.java)
    } else {
        startInternetJob14(team, mediaHash, UploadTeamMediaJob14::class.java)
    }
}
