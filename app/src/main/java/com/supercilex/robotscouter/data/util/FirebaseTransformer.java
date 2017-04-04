package com.supercilex.robotscouter.data.util;

import android.support.annotation.CallSuper;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public abstract class FirebaseTransformer implements ValueEventListener {
    private final Query mFromQuery;
    private final DatabaseReference mToRef;

    private TaskCompletionSource<DataSnapshot> mCompleteTask = new TaskCompletionSource<>();

    public FirebaseTransformer(Query from, DatabaseReference to) {
        mFromQuery = from;
        mToRef = to;
    }

    protected DatabaseReference getToRef() {
        return mToRef;
    }

    protected abstract Task<Void> transform(DataSnapshot transformSnapshot);

    @CallSuper
    @Override
    public void onDataChange(final DataSnapshot snapshot) {
        transform(snapshot).addOnCompleteListener(task -> {
            if (task.isSuccessful()) mCompleteTask.setResult(snapshot);
            else mCompleteTask.setException(task.getException());
        });
    }

    public Task<DataSnapshot> performTransformation() {
        mFromQuery.addListenerForSingleValueEvent(this);
        return mCompleteTask.getTask();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
        mCompleteTask.setException(error.toException());
    }
}
