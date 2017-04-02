package com.supercilex.robotscouter.data.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

import java.util.HashMap;
import java.util.Map;

public class FirebaseCopier extends FirebaseTransformer {
    public FirebaseCopier(Query from, DatabaseReference to) {
        super(from, to);
    }

    public static Task<Void> copyTo(DataSnapshot copySnapshot, DatabaseReference to) {
        Map<String, Object> values = new HashMap<>();
        deepCopy(values, copySnapshot);
        return to.updateChildren(values);
    }

    private static void deepCopy(Map<String, Object> values, DataSnapshot from) {
        Iterable<DataSnapshot> children = from.getChildren();
        if (children.iterator().hasNext()) {
            for (DataSnapshot snapshot : children) {
                Map<String, Object> data = new HashMap<>();
                data.put(".priority", snapshot.getPriority());
                values.put(snapshot.getKey(), data);

                deepCopy(data, snapshot);
            }
        } else {
            values.put(".value", from.getValue());
        }
    }

    @Override
    public Task<Void> transform(DataSnapshot copySnapshot) {
        if (copySnapshot.getValue() == null) return Tasks.forResult(null);
        else return copyTo(copySnapshot, getToRef());
    }
}
