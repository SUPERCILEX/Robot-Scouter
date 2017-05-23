package com.supercilex.robotscouter.data.client

import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader
import com.supercilex.robotscouter.data.util.TeamHelper

class UploadTeamMediaJob14 : TbaJobBase14() {
    override val updateTeam: (helper: TeamHelper, newTeam: Team) -> Unit
        get() = { helper, newTeam -> helper.updateMedia(newTeam) }

    override fun startTask(previousTeam: Team): Task<Team> = TbaUploader.upload(previousTeam, this)
}
