package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class UploadTeamMediaJob21 : TbaJobBase21() {
    override fun startTask(previousTeam: Team) = TbaUploader.upload(previousTeam, this)
}
