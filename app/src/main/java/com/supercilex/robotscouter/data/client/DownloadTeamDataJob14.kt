package com.supercilex.robotscouter.data.client

import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.data.util.TeamHelper

class DownloadTeamDataJob14 : TbaJobBase14() {
    override val updateTeam: (helper: TeamHelper, newTeam: Team) -> Unit
        get() = { helper, newTeam -> helper.updateTeam(newTeam) }

    override fun startTask(previousTeam: Team) = TbaDownloader.load(previousTeam, this)
}
