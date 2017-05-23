package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.data.util.TeamHelper

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DownloadTeamDataJob21 : TbaJobBase21() {
    override val updateTeam: (helper: TeamHelper, newTeam: Team) -> Unit
        get() = { helper, newTeam -> helper.updateTeam(newTeam) }

    override fun startTask(previousTeam: Team) = TbaDownloader.load(previousTeam, this)
}
