package com.supercilex.robotscouter.data.client

import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader
import com.supercilex.robotscouter.util.updateMedia

class UploadTeamMediaJob14 : TbaJobBase14() {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.updateMedia(newTeam) }

    override fun startTask(previousTeam: Team): Task<Team> = TbaUploader.upload(previousTeam, this)
}
