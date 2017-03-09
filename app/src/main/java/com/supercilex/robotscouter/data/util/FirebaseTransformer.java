package com.supercilex.robotscouter.data.util;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public abstract class FirebaseTransformer implements ValueEventListener {
    protected final Query mToQuery;
    private Query mFromQuery;

    private TaskCompletionSource<List<Task<DatabaseReference>>> mCompleteTask = new TaskCompletionSource<>();
    private List<Task<DatabaseReference>> mTransformTasks = new ArrayList<>();

    public FirebaseTransformer(Query to) {
        mToQuery = to;
    }

    public FirebaseTransformer(Query from, Query to) {
        mFromQuery = from;
        mToQuery = to;
    }

    protected abstract Task<Void> transform(DataSnapshot transformSnapshot);

    @CallSuper
    @Override
    public void onDataChange(final DataSnapshot snapshot) {
        for (DataSnapshot snapshotToTransform : snapshot.getChildren()) {
            mTransformTasks.add(transform(snapshotToTransform).continueWith(new Continuation<Void, DatabaseReference>() {
                @Override
                public DatabaseReference then(@NonNull Task<Void> task) throws Exception {
                    return snapshot.getRef();
                }
            }));
        }
        mCompleteTask.setResult(mTransformTasks);
    }

    public void setFromQuery(Query from) {
        mFromQuery = from;
    }

    public Task<List<Task<DatabaseReference>>> performTransformation() {
        mFromQuery.addListenerForSingleValueEvent(this);
        return mCompleteTask.getTask();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
        mCompleteTask.setException(error.toException());
    }
}
