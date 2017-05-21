package com.supercilex.robotscouter.data.client

import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader

class UploadTeamMediaJob14 : TbaJobBase14() {
    override fun startTask(previousTeam: Team): Task<Team> = TbaUploader.upload(previousTeam, this)
}
