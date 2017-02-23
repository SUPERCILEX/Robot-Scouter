package com.supercilex.robotscouter.util;

import android.os.Bundle;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class DatabaseHelper {
    private static final String QUERY_KEY = "query_key";

    private DatabaseHelper() { // no instance
    }

    public static FirebaseDatabase getDatabase() {
        return DatabaseHolder.DATABASE;
    }

    public static DatabaseReference getRef() {
        return DatabaseHolder.INSTANCE;
    }

    public static Bundle getRefBundle(DatabaseReference ref) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_KEY, ref.toString().split("firebaseio.com/")[1]);
        return bundle;
    }

    public static DatabaseReference getRef(Bundle bundle) {
        return DatabaseHelper.getDatabase().getReference(bundle.getString(QUERY_KEY));
    }

    private static final class DatabaseHolder {
        public static final FirebaseDatabase DATABASE = FirebaseDatabase.getInstance();
        public static final DatabaseReference INSTANCE;

        static {
            DATABASE.setPersistenceEnabled(true);
            INSTANCE = DATABASE.getReference();
        }
    }
}
