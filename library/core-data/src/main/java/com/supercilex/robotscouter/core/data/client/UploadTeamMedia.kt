package com.supercilex.robotscouter.core.data.client

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.updateMedia
import com.supercilex.robotscouter.core.data.parseTeam
import com.supercilex.robotscouter.core.data.remote.TbaMediaUploader
import com.supercilex.robotscouter.core.data.remote.TeamMediaUploader
import com.supercilex.robotscouter.core.data.toWorkData
import com.supercilex.robotscouter.core.model.Team

internal const val TEAM_MEDIA_UPLOAD = "team_media_upload"

internal fun Team.startUploadMediaJob() {
    WorkManager.getInstance().beginUniqueWork(
            checkNotNull(media),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UploadTeamMediaWorker>()
                    .addTag(TEAM_MEDIA_UPLOAD)
                    .setConstraints(Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.UNMETERED)
                                            .build())
                    .setInputData(toWorkData())
                    .build()
    ).then(
            OneTimeWorkRequestBuilder<UploadTbaMediaWorker>()
                    .setConstraints(Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build())
                    .build()
    ).synchronous().enqueueSync()
}

internal class UploadTeamMediaWorker : TeamWorker() {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.updateMedia(newTeam) }

    override fun startTask(latestTeam: Team, originalTeam: Team) =
            TeamMediaUploader.upload(latestTeam.apply { copyMediaInfo(originalTeam) })
}

internal class UploadTbaMediaWorker : WorkerBase() {
    override suspend fun doBlockingWork(): Result {
        TbaMediaUploader.upload(inputData.parseTeam())
        return Result.SUCCESS
    }
}
