package com.supercilex.robotscouter.core.data.client

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.forceUpdate
import com.supercilex.robotscouter.core.data.parseTeam
import com.supercilex.robotscouter.core.data.remote.TbaMediaUploader
import com.supercilex.robotscouter.core.data.remote.TeamMediaUploader
import com.supercilex.robotscouter.core.data.toWorkData
import com.supercilex.robotscouter.core.model.Team

internal const val TEAM_MEDIA_UPLOAD = "team_media_upload"
private const val SHOULD_UPLOAD_KEY = "should_upload_media"

private val localMediaPrefs: SharedPreferences by lazy {
    RobotScouter.getSharedPreferences("local_media_prefs", Context.MODE_PRIVATE)
}

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
    ).enqueue()
}

internal fun Team.retrieveLocalMedia() = localMediaPrefs.getString(id, null)

internal fun Team.retrieveShouldUpload() = localMediaPrefs.getBoolean(id + SHOULD_UPLOAD_KEY, false)

internal fun Team.saveLocalMedia() {
    localMediaPrefs.edit(true) {
        putString(id, checkNotNull(media))
        putBoolean(id + SHOULD_UPLOAD_KEY, shouldUploadMediaToTba)
    }
}

private fun Team.deleteLocalMedia() {
    localMediaPrefs.edit {
        remove(id)
        remove(id + SHOULD_UPLOAD_KEY)
    }
}

internal class UploadTeamMediaWorker(
        context: Context,
        workerParams: WorkerParameters
) : TeamWorker(context, workerParams) {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam ->
            team.copyMediaInfo(newTeam)
            team.forceUpdate()
            team.deleteLocalMedia()
        }

    override suspend fun startTask(latestTeam: Team, originalTeam: Team) =
            TeamMediaUploader.upload(latestTeam.apply { copyMediaInfo(originalTeam) })
}

internal class UploadTbaMediaWorker(
        context: Context,
        workerParams: WorkerParameters
) : WorkerBase(context, workerParams) {
    override suspend fun doBlockingWork(): Result {
        TbaMediaUploader.upload(inputData.parseTeam())
        return Result.success()
    }
}
