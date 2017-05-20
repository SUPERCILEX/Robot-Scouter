package com.supercilex.robotscouter.data.client

import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader

class UploadTeamMediaJob14 : TbaJobBase14() {
    override fun startTask(previousTeam: Team) = TbaUploader.upload(previousTeam, this)
}
