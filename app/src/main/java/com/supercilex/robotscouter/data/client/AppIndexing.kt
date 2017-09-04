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
import com.supercilex.robotscouter.util.data.TemplateNamesLiveData
import com.supercilex.robotscouter.util.data.model.getTemplateIndexable
import com.supercilex.robotscouter.util.data.model.indexable
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.onSignedIn
import java.util.concurrent.ExecutionException

class AppIndexingService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        try {
            Tasks.await(Tasks.whenAll(onSignedIn(), FirebaseAppIndex.getInstance().removeAll()))
            Tasks.await(Tasks.whenAll(
                    TeamsLiveData.observeOnDataChanged().observeOnce {
                        async {
                            FirebaseAppIndex.getInstance().update(*it.mapIndexed { index, _ ->
                                it.getObject(index).indexable
                            }.toTypedArray())
                        }
                    },
                    TemplateNamesLiveData.observeOnDataChanged().observeOnce {
                        async {
                            FirebaseAppIndex.getInstance().update(*it.mapIndexed { index, snapshot ->
                                getTemplateIndexable(snapshot.key, it.getObject(index))
                            }.toTypedArray())
                        }
                    }))
        } catch (e: ExecutionException) {
            FirebaseCrash.report(e)
        }
    }

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
