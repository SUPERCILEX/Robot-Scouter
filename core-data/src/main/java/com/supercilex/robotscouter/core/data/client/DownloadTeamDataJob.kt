package com.supercilex.robotscouter.core.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.core.data.model.isStale
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.data.remote.TbaDownloader
import com.supercilex.robotscouter.core.data.startInternetJob14
import com.supercilex.robotscouter.core.data.startInternetJob21
import com.supercilex.robotscouter.core.model.Team

internal fun Team.startDownloadDataJob() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(number.toInt(), DownloadTeamDataJob21::class.java)
    } else {
        startInternetJob14(number.toInt(), DownloadTeamDataJob14::class.java)
    }
}

private interface DownloadTeamDataJob : TeamJob {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.update(newTeam) }

    override fun startTask(
            originalTeam: Team,
            existingFetchedTeam: Team
    ) = if (existingFetchedTeam.isStale) {
        TbaDownloader.load(existingFetchedTeam)
    } else {
        null
    }
}

internal class DownloadTeamDataJob14 : TbaJobBase14(), DownloadTeamDataJob

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class DownloadTeamDataJob21 : TbaJobBase21(), DownloadTeamDataJob
