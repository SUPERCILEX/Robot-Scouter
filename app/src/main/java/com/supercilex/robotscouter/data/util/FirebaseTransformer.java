package com.supercilex.robotscouter.data.util;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public abstract class FirebaseTransformer implements ValueEventListener {
    protected final Query mToQuery;
    protected Query mFromQuery;

    public FirebaseTransformer(Query to) {
        mToQuery = to;
    }

    public FirebaseTransformer(Query from, Query to) {
        mFromQuery = from;
        mToQuery = to;
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        for (DataSnapshot snapshotToTransform : snapshot.getChildren()) {
            transform(snapshotToTransform);
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }

    public void setFromQuery(Query from) {
        mFromQuery = from;
    }

    public void performTransformation() {
        mFromQuery.addListenerForSingleValueEvent(this);
    }

    protected abstract void transform(DataSnapshot transformSnapshot);
}
