package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader
import com.supercilex.robotscouter.util.data.model.updateMedia
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

interface UploadTeamMediaJob : TeamJob {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.updateMedia(newTeam) }

    override fun startTask(previousTeam: Team): Task<Team> = TbaUploader.upload(previousTeam)
}

class UploadTeamMediaJob14 : TbaJobBase14(), UploadTeamMediaJob

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class UploadTeamMediaJob21 : TbaJobBase21(), UploadTeamMediaJob
