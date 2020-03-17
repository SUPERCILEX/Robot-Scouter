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
import androidx.work.workDataOf
import com.supercilex.robotscouter.common.FIRESTORE_ID
import com.supercilex.robotscouter.common.FIRESTORE_MEDIA
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.remote.TeamMediaUploader
import com.supercilex.robotscouter.core.model.Team

internal const val TEAM_MEDIA_UPLOAD = "team_media_upload"
private const val SHOULD_UPLOAD_KEY = "should_upload_media"

private val localMediaPrefs: SharedPreferences by lazy {
    RobotScouter.getSharedPreferences("local_media_prefs", Context.MODE_PRIVATE)
}

fun initWork() {
    localMediaPrefs
}

internal fun startUploadMediaJob(teamId: String, teamName: String, media: String) {
    WorkManager.getInstance(RobotScouter).beginUniqueWork(
            media,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<UploadTeamMediaWorker>()
                    .addTag(TEAM_MEDIA_UPLOAD)
                    .setConstraints(Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.UNMETERED)
                                            .build())
                    .setInputData(workDataOf(
                            FIRESTORE_ID to teamId,
                            FIRESTORE_NAME to teamName,
                            FIRESTORE_MEDIA to media,
                            SHOULD_UPLOAD_KEY to retrieveShouldUpload(teamId)
                    ))
                    .build()
    ).enqueue()
}

internal fun Team.retrieveLocalMedia() = localMediaPrefs.getString(id, null)

internal fun Team.saveLocalMedia() {
    localMediaPrefs.edit(true) {
        putString(id, checkNotNull(media))
        putBoolean(id + SHOULD_UPLOAD_KEY, shouldUploadMediaToTba)
    }
}

private fun retrieveShouldUpload(teamId: String) =
        localMediaPrefs.getBoolean(teamId + SHOULD_UPLOAD_KEY, false)

private fun deleteLocalMedia(teamId: String) {
    localMediaPrefs.edit(true) {
        remove(teamId)
        remove(teamId + SHOULD_UPLOAD_KEY)
    }
}

internal class UploadTeamMediaWorker(
        context: Context,
        workerParams: WorkerParameters
) : WorkerBase(context, workerParams) {
    override suspend fun doBlockingWork(): Result {
        val teamId = checkNotNull(inputData.getString(FIRESTORE_ID))
        TeamMediaUploader(
                teamId,
                checkNotNull(inputData.getString(FIRESTORE_NAME)),
                checkNotNull(inputData.getString(FIRESTORE_MEDIA)),
                inputData.getBoolean(SHOULD_UPLOAD_KEY, false)
        ).execute()
        deleteLocalMedia(teamId)

        return Result.success()
    }
}
