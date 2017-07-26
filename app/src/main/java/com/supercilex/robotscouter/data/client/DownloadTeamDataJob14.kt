package com.supercilex.robotscouter.data.client

import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.util.updateTeam

class DownloadTeamDataJob14 : TbaJobBase14() {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.updateTeam(newTeam) }

    override fun startTask(previousTeam: Team) = TbaDownloader.load(previousTeam, this)
}
