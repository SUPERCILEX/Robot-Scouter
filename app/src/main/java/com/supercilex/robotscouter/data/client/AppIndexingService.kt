package com.supercilex.robotscouter.data.client

import android.app.IntentService
import android.content.Intent
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.ChangeEventListenerBase
import com.supercilex.robotscouter.util.observeOnce
import com.supercilex.robotscouter.util.onSignedIn
import com.supercilex.robotscouter.util.teamsListener
import java.util.ArrayList

class AppIndexingService : IntentService(TAG),
        OnSuccessListener<ObservableSnapshotArray<Team>>, ValueEventListener {
    private val indexables = ArrayList<Indexable>()
    private var numOfExpectedTeams = 0L

    init {
        setIntentRedelivery(true)
    }

    override fun onHandleIntent(intent: Intent) {
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        if (availability == ConnectionResult.SUCCESS) {
            onSignedIn().addOnSuccessListener {
                TeamHelper.getIndicesRef().addListenerForSingleValueEvent(this)
            }
        } else {
            GoogleApiAvailability.getInstance().showErrorNotification(this, availability)
        }
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        numOfExpectedTeams = snapshot.childrenCount
        if (numOfExpectedTeams == 0L) {
            FirebaseAppIndex.getInstance().update()
            return
        }

        teamsListener.observeOnce(false).addOnSuccessListener(this)
    }

    override fun onSuccess(teams: ObservableSnapshotArray<Team>) {
        teams.addChangeEventListener(object : ChangeEventListenerBase() {
            override fun onChildChanged(type: ChangeEventListener.EventType,
                                        snapshot: DataSnapshot,
                                        index: Int,
                                        oldIndex: Int) {
                if (type == ChangeEventListener.EventType.ADDED) {
                    indexables.add(teams.getObject(index).helper.indexable)
                }

                if (indexables.size >= numOfExpectedTeams) {
                    FirebaseAppIndex.getInstance().update(*indexables.toTypedArray())
                    teams.removeChangeEventListener(this)
                }
            }
        })
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())

    companion object {
        private const val TAG = "AppIndexingService"
    }
}
