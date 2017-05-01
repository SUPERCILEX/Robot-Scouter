package com.supercilex.robotscouter.data.client;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.DatabaseHelper.ChangeEventListenerBase;

import java.util.ArrayList;
import java.util.List;

public class AppIndexingService extends IntentService implements OnSuccessListener<FirebaseAuth>, ValueEventListener {
    private static final String TAG = "AppIndexingService";

    private final List<Indexable> mIndexables = new ArrayList<>();

    public AppIndexingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int result = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(getApplicationContext());

        if (result == ConnectionResult.SUCCESS) {
            AuthHelper.onSignedIn().addOnSuccessListener(this);
        } else {
            GoogleApiAvailability.getInstance().showErrorNotification(this, result);
        }
    }

    @Override
    public void onSuccess(FirebaseAuth result) {
        TeamHelper.getIndicesRef().addListenerForSingleValueEvent(this);
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        long numOfExpectedTeams = snapshot.getChildrenCount();
        if (numOfExpectedTeams == 0) {
            FirebaseAppIndex.getInstance().update();
            return;
        }

        Constants.sFirebaseTeams.addChangeEventListener(new ChangeEventListenerBase() {
            @Override
            public void onChildChanged(EventType type,
                                       DataSnapshot snapshot,
                                       int index,
                                       int oldIndex) {
                if (type == EventType.ADDED) {
                    mIndexables.add(Constants.sFirebaseTeams.getObject(index)
                                            .getHelper()
                                            .getIndexable());
                }

                if (mIndexables.size() >= numOfExpectedTeams) {
                    FirebaseAppIndex.getInstance()
                            .update(mIndexables.toArray(new Indexable[mIndexables.size()]));
                    Constants.sFirebaseTeams.removeChangeEventListener(this);
                }
            }
        });
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }
}
