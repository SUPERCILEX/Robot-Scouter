package com.supercilex.robotscouter.util;

import android.content.Context;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtils {
    private static FirebaseDatabase sDatabase;
    private static FirebaseAuth sAuth;
    private static FirebaseJobDispatcher sDispatcher;

    public static DatabaseReference getDatabase() {
        if (sDatabase == null) {
            sDatabase = FirebaseDatabase.getInstance();
            sDatabase.setPersistenceEnabled(true);
        }
        return sDatabase.getReference();
    }

    public static FirebaseAuth getAuth() {
        if (sAuth == null) {
            sAuth = FirebaseAuth.getInstance();
        }
        return sAuth;
    }

    public static FirebaseUser getUser() {
        return getAuth().getCurrentUser();
    }

    public static String getUid() {
        return getUser().getUid();
    }

    public static FirebaseJobDispatcher getDispatcher() {
        if (sDispatcher == null) {
            throw new IllegalStateException("FirebaseJobDispatcher was null");
        }
        return sDispatcher;
    }

    public static FirebaseJobDispatcher getDispatcher(Context context) {
        if (sDispatcher == null) {
            sDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        }
        return sDispatcher;
    }
}
