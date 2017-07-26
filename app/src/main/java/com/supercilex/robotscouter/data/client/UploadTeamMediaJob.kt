package com.supercilex.robotscouter.data.client

import android.content.Context
import android.os.Build
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.startInternetJob14
import com.supercilex.robotscouter.util.startInternetJob21

fun startUploadTeamMediaJob(context: Context, team: Team) {
    val mediaHash: Int = team.media!!.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(context,
                team,
                mediaHash,
                UploadTeamMediaJob21::class.java)
    } else {
        startInternetJob14(context,
                team,
                mediaHash,
                UploadTeamMediaJob14::class.java)
    }
}
