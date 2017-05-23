package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader
import com.supercilex.robotscouter.data.util.TeamHelper

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class UploadTeamMediaJob21 : TbaJobBase21() {
    override val updateTeam: (helper: TeamHelper, newTeam: Team) -> Unit
        get() = { helper, newTeam -> helper.updateMedia(newTeam) }

    override fun startTask(previousTeam: Team): Task<Team> = TbaUploader.upload(previousTeam, this)
}
