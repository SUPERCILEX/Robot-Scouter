package com.supercilex.robotscouter.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.supercilex.robotscouter.BuildConfig;

public final class DatabaseHelper {
    private DatabaseHelper() {
        // no instance
    }

    public static DatabaseReference getReference() {
        return DatabaseHolder.INSTANCE;
    }

    private static final class DatabaseHolder {
        public static final DatabaseReference INSTANCE;
        private static final String DATABASE_VERSION = "v" + BuildConfig.VERSION_NAME.split("\\.")[0];

        static {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
            INSTANCE = database.getReference()
                    .child(BuildConfig.DEBUG ? "debug" : DATABASE_VERSION);
        }
    }
}
