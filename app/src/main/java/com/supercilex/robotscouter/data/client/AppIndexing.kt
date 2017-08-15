package com.supercilex.robotscouter.data.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.model.indexable
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.teamsListener
import java.util.concurrent.ExecutionException

class AppIndexingService : JobIntentService(), OnSuccessListener<ObservableSnapshotArray<Team>> {
    override fun onHandleWork(intent: Intent) {
        try {
            Tasks.await(onSignedIn())
            Tasks.await(teamsListener.observeOnDataChanged().observeOnce()
                                .addOnSuccessListener(this))
        } catch (e: ExecutionException) {
            FirebaseCrash.report(e)
        }
    }

    override fun onSuccess(teams: ObservableSnapshotArray<Team>) {
        FirebaseAppIndex.getInstance().apply {
            removeAll()
            update(*teams.mapIndexed { index, _ -> teams.getObject(index).indexable }.toTypedArray())
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
