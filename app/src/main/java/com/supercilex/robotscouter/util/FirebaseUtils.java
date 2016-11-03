package com.supercilex.robotscouter.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtils {
    private static FirebaseDatabase sDatabase;

    public static DatabaseReference getDatabase() {
        if (sDatabase == null) {
            sDatabase = FirebaseDatabase.getInstance();
            sDatabase.setPersistenceEnabled(true);
        }
        return sDatabase.getReference();
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseUser getUser() {
        return getAuth().getCurrentUser();
    }

    public static String getUid() {
        return getUser().getUid();
    }
}
