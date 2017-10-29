package com.supercilex.robotscouter.data.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.getTemplateIndexable
import com.supercilex.robotscouter.util.data.indexable
import com.supercilex.robotscouter.util.data.model.getTemplateName
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.data.scoutParser
import com.supercilex.robotscouter.util.onSignedIn
import java.util.concurrent.ExecutionException

class AppIndexingService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        try {
            Tasks.await(Tasks.whenAll(onSignedIn(), FirebaseAppIndex.getInstance().removeAll()))
            Tasks.await(Tasks.whenAll(getUpdateTeamsTask(), getUpdateTemplatesTask()))
        } catch (e: ExecutionException) {
            FirebaseCrash.report(e)
        }
    }

    private fun getUpdateTeamsTask() = FirebaseAppIndex.getInstance().update(
            *Tasks.await(TeamsLiveData.observeOnDataChanged().observeOnce {
                async { it.map { it.indexable }.toTypedArray() }
            }))

    private fun getUpdateTemplatesTask() = FirebaseAppIndex.getInstance().update(
            *Tasks.await(getTemplatesQuery().get()).mapIndexed { index, snapshot ->
                getTemplateIndexable(
                        snapshot.id,
                        scoutParser.parseSnapshot(snapshot).getTemplateName(index))
            }.toTypedArray())

    companion object {
        private const val JOB_ID = 387

        fun refreshIndexables(context: Context) =
                enqueueWork(context, AppIndexingService::class.java, JOB_ID, Intent())
    }
}

class AppIndexingUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && intent.action == FirebaseAppIndex.ACTION_UPDATE_INDEX) {
            AppIndexingService.refreshIndexables(context)
        }
    }
}
