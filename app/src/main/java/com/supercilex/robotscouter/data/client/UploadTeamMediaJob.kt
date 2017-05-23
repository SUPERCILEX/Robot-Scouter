package com.supercilex.robotscouter.data.client

import android.content.Context
import android.os.Build
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.startInternetJob14
import com.supercilex.robotscouter.util.startInternetJob21

fun startUploadTeamMediaJob(context: Context, teamHelper: TeamHelper) {
    val mediaHash: Int = teamHelper.team.media.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(context,
                teamHelper,
                mediaHash,
                UploadTeamMediaJob21::class.java)
    } else {
        startInternetJob14(context,
                teamHelper,
                mediaHash,
                UploadTeamMediaJob14::class.java)
    }
}
