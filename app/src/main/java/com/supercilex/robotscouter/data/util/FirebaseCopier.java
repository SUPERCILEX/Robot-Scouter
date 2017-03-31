package com.supercilex.robotscouter.data.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.List;

public class FirebaseCopier extends FirebaseTransformer {
    private List<Task<Void>> mWriteTasks = new ArrayList<>();

    public FirebaseCopier(Query to) {
        super(to);
    }

    public FirebaseCopier(Query from, Query to) {
        super(from, to);
    }

    @Override
    public Task<Void> transform(DataSnapshot copySnapshot) {
        DatabaseReference to = mToQuery.getRef().child(copySnapshot.getKey());
        to.setValue(copySnapshot.getValue(), copySnapshot.getPriority());
        deepCopy(copySnapshot, to);
        return Tasks.whenAll(mWriteTasks);
    }

    private void deepCopy(DataSnapshot from, DatabaseReference to) {
        Iterable<DataSnapshot> children = from.getChildren();
        if (children.iterator().hasNext()) {
            for (DataSnapshot snapshot : children) {
                deepCopy(snapshot, to.child(snapshot.getKey()));
            }
        } else {
            mWriteTasks.add(to.setValue(from.getValue(), from.getPriority()));
        }
    }
}
