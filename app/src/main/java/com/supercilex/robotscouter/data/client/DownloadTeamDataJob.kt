package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.util.data.model.update
import com.supercilex.robotscouter.util.data.startInternetJob14
import com.supercilex.robotscouter.util.data.startInternetJob21

fun startDownloadTeamDataJob(team: Team) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(team, team.number.toInt(), DownloadTeamDataJob21::class.java)
    } else {
        startInternetJob14(team, team.number.toInt(), DownloadTeamDataJob14::class.java)
    }
}

interface DownloadTeamDataJob : TeamJob {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.update(newTeam) }

    override fun startTask(existingTeam: Team) = TbaDownloader.load(existingTeam)
}

class DownloadTeamDataJob14 : TbaJobBase14(), DownloadTeamDataJob

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DownloadTeamDataJob21 : TbaJobBase21(), DownloadTeamDataJob
