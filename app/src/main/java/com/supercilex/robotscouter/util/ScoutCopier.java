package com.supercilex.robotscouter.util;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.ScoutMetric;

public abstract class ScoutCopier implements ValueEventListener {
    private Query mCopyToQuery;

    public ScoutCopier(Query copyToQuery) {
        mCopyToQuery = copyToQuery;
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        Scout scoutCopy = new Scout();
        for (DataSnapshot scout : snapshot.child(Constants.FIREBASE_VIEWS).getChildren()) {
            scoutCopy.addView(scout.getKey(), ScoutMetric.getMetric(scout));
        }
        mCopyToQuery.getRef().setValue(scoutCopy);
        onDone();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }

    protected abstract void onDone();
}
