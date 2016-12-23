package com.supercilex.robotscouter.data.util;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Query;

public class TeamCopier extends FirebaseCopier {
    public TeamCopier(Query to) {
        super(to);
    }

    public TeamCopier(Query from, Query to) {
        super(from, to);
    }

    @Override
    public void transform(DataSnapshot copySnapshot) {
        mToQuery.getRef()
                .child(copySnapshot.getKey())
                .setValue(copySnapshot.getValue(),
                          copySnapshot.getValue()); // TODO: 12/22/2016 test copying
    }
}
