package com.supercilex.robotscouter.data.client

import android.app.IntentService
import android.content.Intent
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.appindexing.FirebaseAppIndex
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.model.indexable
import com.supercilex.robotscouter.util.data.observeOnDataChanged
import com.supercilex.robotscouter.util.data.observeOnce
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.teamsListener

class AppIndexingService : IntentService(TAG),
        OnSuccessListener<ObservableSnapshotArray<Team>> {
    init {
        setIntentRedelivery(true)
    }

    override fun onHandleIntent(intent: Intent) {
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        if (availability == ConnectionResult.SUCCESS) {
            onSignedIn().addOnSuccessListener {
                teamsListener.observeOnDataChanged().observeOnce().addOnSuccessListener(this)
            }
        } else {
            GoogleApiAvailability.getInstance().showErrorNotification(this, availability)
        }
    }

    override fun onSuccess(teams: ObservableSnapshotArray<Team>) {
        FirebaseAppIndex.getInstance().apply {
            update(*teams.mapIndexed { index, _ -> teams.getObject(index).indexable }.toTypedArray())
        }
    }

    companion object {
        private const val TAG = "AppIndexingService"
    }
}
