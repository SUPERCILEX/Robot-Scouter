package com.supercilex.robotscouter.core.data

import androidx.work.WorkManager
import androidx.work.await
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.client.TEAM_MEDIA_UPLOAD

internal suspend fun cleanupJobs() {
    WorkManager.getInstance(RobotScouter).apply {
        listOf(TEAM_MEDIA_UPLOAD)
                .map { cancelAllWorkByTag(it) }
                .forEach { it.result.await() }
        pruneWork()
    }
}
