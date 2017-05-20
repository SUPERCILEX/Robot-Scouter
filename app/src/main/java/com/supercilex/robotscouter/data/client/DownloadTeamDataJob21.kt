package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DownloadTeamDataJob21 : TbaJobBase21() {
    override fun startTask(previousTeam: Team) = TbaDownloader.load(previousTeam, this)
}
