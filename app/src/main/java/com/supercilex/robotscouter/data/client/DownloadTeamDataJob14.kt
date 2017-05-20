package com.supercilex.robotscouter.data.client

import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader

class DownloadTeamDataJob14 : TbaJobBase14() {
    override fun startTask(previousTeam: Team) = TbaDownloader.load(previousTeam, this)
}
