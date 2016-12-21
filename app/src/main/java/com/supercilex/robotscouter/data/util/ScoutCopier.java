package com.supercilex.robotscouter.data.util;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.ScoutMetric;
import com.supercilex.robotscouter.util.Constants;

public abstract class ScoutCopier extends FirebaseCopier {
    private Scout mScoutCopy;

    public ScoutCopier(Query to) {
        super(to);
        mScoutCopy = new Scout();
    }

    public ScoutCopier(Query from, Query to) {
        super(getViewsRef(from), to);
        mScoutCopy = new Scout();
    }

    private static DatabaseReference getViewsRef(Query from) {
        return from.getRef().child(Constants.FIREBASE_VIEWS);
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        super.onDataChange(snapshot);
        mToQuery.getRef().setValue(mScoutCopy);
        onDone();
    }

    @Override
    public void transform(DataSnapshot scout) {
        mScoutCopy.addView(scout.getKey(), ScoutMetric.getMetric(scout));
    }

    @Override
    public void setFromQuery(Query from) {
        super.setFromQuery(getViewsRef(from));
    }

    protected abstract void onDone();
}
