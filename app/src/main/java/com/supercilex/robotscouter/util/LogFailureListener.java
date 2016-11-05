package com.supercilex.robotscouter.util;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.crash.FirebaseCrash;

public class LogFailureListener implements OnFailureListener {
    @Override
    public void onFailure(@NonNull Exception e) {
        if (!(e instanceof FirebaseNetworkException)) FirebaseCrash.report(e);
    }
}
