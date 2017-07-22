package com.supercilex.robotscouter.data.client

import android.app.IntentService
import android.content.Intent
import com.firebase.ui.database.ChangeEventListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.onSignedIn
import java.util.ArrayList

class AppIndexingService : IntentService(TAG), OnSuccessListener<FirebaseAuth>, ValueEventListener {
    private val indexables = ArrayList<Indexable>()

    init {
        setIntentRedelivery(true)
    }

    override fun onHandleIntent(intent: Intent) {
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        if (availability == ConnectionResult.SUCCESS) {
            onSignedIn().addOnSuccessListener(this)
        } else {
            GoogleApiAvailability.getInstance().showErrorNotification(this, availability)
        }
    }

    override fun onSuccess(result: FirebaseAuth) =
            TeamHelper.getIndicesRef().addListenerForSingleValueEvent(this)

    override fun onDataChange(snapshot: DataSnapshot) {
        val numOfExpectedTeams = snapshot.childrenCount
        if (numOfExpectedTeams == 0L) {
            FirebaseAppIndex.getInstance().update()
            return
        }

        Constants.sFirebaseTeams.addChangeEventListener(object : ChangeEventListenerBase() {
            override fun onChildChanged(type: ChangeEventListener.EventType,
                                        snapshot: DataSnapshot,
                                        index: Int,
                                        oldIndex: Int) {
                if (type == ChangeEventListener.EventType.ADDED) {
                    indexables.add(Constants.sFirebaseTeams.getObject(index).helper.indexable)
                }

                if (indexables.size >= numOfExpectedTeams) {
                    FirebaseAppIndex.getInstance().update(*indexables.toTypedArray())
                    Constants.sFirebaseTeams.removeChangeEventListener(this)
                }
            }
        })
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())

    companion object {
        private const val TAG = "AppIndexingService"
    }
}
