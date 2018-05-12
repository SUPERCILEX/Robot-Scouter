package com.supercilex.robotscouter.shared.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import com.google.firebase.appindexing.FirebaseAppIndex
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.getTemplateIndexable
import com.supercilex.robotscouter.core.data.indexable
import com.supercilex.robotscouter.core.data.model.getTemplateName
import com.supercilex.robotscouter.core.data.model.getTemplatesQuery
import com.supercilex.robotscouter.core.data.model.scoutParser
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.waitForChange
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking

internal class AppIndexingService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        runBlocking {
            try {
                onSignedIn()
                FirebaseAppIndex.getInstance().removeAll().await()

                await(async { getUpdateTeamsTask() }, async { getUpdateTemplatesTask() })
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
            }
        }
    }

    private suspend fun getUpdateTeamsTask() {
        val indexables = teams.waitForChange().map { it.indexable }
        FirebaseAppIndex.getInstance().update(*indexables.toTypedArray()).await()
    }

    private suspend fun getUpdateTemplatesTask() {
        val indexables = getTemplatesQuery().get().await().mapIndexed { index, snapshot ->
            getTemplateIndexable(
                    snapshot.id,
                    scoutParser.parseSnapshot(snapshot).getTemplateName(index)
            )
        }
        FirebaseAppIndex.getInstance().update(*indexables.toTypedArray()).await()
    }

    companion object {
        private const val JOB_ID = 387

        fun refreshIndexables() =
                enqueueWork(RobotScouter, AppIndexingService::class.java, JOB_ID, Intent())
    }
}

internal class AppIndexingUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == FirebaseAppIndex.ACTION_UPDATE_INDEX) {
            AppIndexingService.refreshIndexables()
        }
    }
}
