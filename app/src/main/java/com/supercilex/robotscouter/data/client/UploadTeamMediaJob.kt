package com.supercilex.robotscouter.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.firebase.jobdispatcher.Lifetime
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaUploader
import com.supercilex.robotscouter.util.data.model.copyMediaInfo
import com.supercilex.robotscouter.util.data.model.updateMedia
import com.supercilex.robotscouter.util.data.startInternetJob14
import com.supercilex.robotscouter.util.data.startInternetJob21
import java.io.File

fun Team.startUploadMediaJob() {
    val mediaHash: Int = media!!.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(mediaHash, UploadTeamMediaJob21::class.java) {
            setPersisted(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setEstimatedNetworkBytes(File(media).length())
            }
        }
    } else {
        startInternetJob14(mediaHash, UploadTeamMediaJob14::class.java) {
            lifetime = Lifetime.FOREVER
        }
    }
}

interface UploadTeamMediaJob : TeamJob {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.updateMedia(newTeam) }

    override fun startTask(originalTeam: Team, existingFetchedTeam: Team) =
            TbaUploader.upload(existingFetchedTeam.apply { copyMediaInfo(originalTeam) })
}

class UploadTeamMediaJob14 : TbaJobBase14(), UploadTeamMediaJob

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class UploadTeamMediaJob21 : TbaJobBase21(), UploadTeamMediaJob
