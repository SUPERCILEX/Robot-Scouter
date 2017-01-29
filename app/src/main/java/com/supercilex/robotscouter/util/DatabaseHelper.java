package com.supercilex.robotscouter.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class DatabaseHelper {
    private DatabaseHelper() {
        // no instance
    }

    public static DatabaseReference getReference() {
        return DatabaseHolder.INSTANCE.getReference();
    }

    private static final class DatabaseHolder {
        public static final FirebaseDatabase INSTANCE = FirebaseDatabase.getInstance();

        static {
            INSTANCE.setPersistenceEnabled(true);
        }
    }
}
