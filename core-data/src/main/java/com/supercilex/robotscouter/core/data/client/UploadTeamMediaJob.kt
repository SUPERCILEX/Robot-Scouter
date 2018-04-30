package com.supercilex.robotscouter.core.data.client

import android.os.Build
import android.support.annotation.RequiresApi
import com.firebase.jobdispatcher.Lifetime
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.updateMedia
import com.supercilex.robotscouter.core.data.remote.TeamMediaUploader
import com.supercilex.robotscouter.core.data.startInternetJob14
import com.supercilex.robotscouter.core.data.startInternetJob21
import com.supercilex.robotscouter.core.model.Team

internal fun Team.startUploadMediaJob() {
    val mediaHash: Int = media!!.hashCode()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        startInternetJob21(mediaHash, UploadTeamMediaJob21::class.java) {
            setPersisted(true)
            // TODO add back once P ships
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                setEstimatedNetworkBytes(File(media).length())
//            }
        }
    } else {
        startInternetJob14(mediaHash, UploadTeamMediaJob14::class.java) {
            lifetime = Lifetime.FOREVER
        }
    }
}

private interface UploadTeamMediaJob : TeamJob {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.updateMedia(newTeam) }

    override fun startTask(originalTeam: Team, existingFetchedTeam: Team) =
            TeamMediaUploader.upload(existingFetchedTeam.apply { copyMediaInfo(originalTeam) })
}

internal class UploadTeamMediaJob14 : TbaJobBase14(), UploadTeamMediaJob

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class UploadTeamMediaJob21 : TbaJobBase21(), UploadTeamMediaJob
