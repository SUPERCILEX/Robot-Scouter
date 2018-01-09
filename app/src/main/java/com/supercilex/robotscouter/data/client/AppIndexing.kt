package com.supercilex.robotscouter.data.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import com.google.firebase.appindexing.FirebaseAppIndex
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.getTemplateIndexable
import com.supercilex.robotscouter.util.data.indexable
import com.supercilex.robotscouter.util.data.model.getTemplateName
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.onSignedIn
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking

class AppIndexingService : JobIntentService() {
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
        val indexables = TeamsLiveData.observeOnDataChanged().observeOnce()?.toList()
                ?.map { it.indexable } ?: return
        FirebaseAppIndex.getInstance().update(*indexables.toTypedArray()).await()
    }

    private suspend fun getUpdateTemplatesTask() {
        val indexables = getTemplatesQuery().log().get().await().mapIndexed { index, snapshot ->
            getTemplateIndexable(
                    snapshot.id,
                    scoutParser.parseSnapshot(snapshot).getTemplateName(index)
            )
        }
        FirebaseAppIndex.getInstance().update(*indexables.toTypedArray()).await()
    }

    companion object {
        private const val JOB_ID = 387

        fun refreshIndexables(context: Context) =
                enqueueWork(context, AppIndexingService::class.java, JOB_ID, Intent())
    }
}

class AppIndexingUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == FirebaseAppIndex.ACTION_UPDATE_INDEX) {
            AppIndexingService.refreshIndexables(context)
        }
    }
}
