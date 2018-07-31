package com.supercilex.robotscouter.core.data.client

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.supercilex.robotscouter.core.data.model.isStale
import com.supercilex.robotscouter.core.data.model.update
import com.supercilex.robotscouter.core.data.remote.TeamDetailsDownloader
import com.supercilex.robotscouter.core.data.toWorkData
import com.supercilex.robotscouter.core.model.Team

internal const val TEAM_DATA_DOWNLOAD = "team_data_download"

internal fun Team.startDownloadDataJob() {
    WorkManager.getInstance().beginUniqueWork(
            number.toString(),
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DownloadTeamDataWorker>()
                    .addTag(TEAM_DATA_DOWNLOAD)
                    .setConstraints(Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build())
                    .setInputData(toWorkData())
                    .build()
    ).synchronous().enqueueSync()
}

internal class DownloadTeamDataWorker : TeamWorker() {
    override val updateTeam: (team: Team, newTeam: Team) -> Unit
        get() = { team, newTeam -> team.update(newTeam) }

    override fun startTask(latestTeam: Team, originalTeam: Team) = if (latestTeam.isStale) {
        TeamDetailsDownloader.load(latestTeam)
    } else {
        null
    }
}
