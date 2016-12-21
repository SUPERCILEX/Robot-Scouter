package com.supercilex.robotscouter.data.util;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;

public class FirebaseCopier extends FirebaseTransformer {
    public FirebaseCopier(Query to) {
        super(to);
    }

    public FirebaseCopier(Query from, Query to) {
        super(from, to);
    }

    @Override
    public void transform(DataSnapshot copySnapshot) {
        mToQuery.getRef().child(copySnapshot.getKey()).setValue(copySnapshot.getValue());
    }
}
