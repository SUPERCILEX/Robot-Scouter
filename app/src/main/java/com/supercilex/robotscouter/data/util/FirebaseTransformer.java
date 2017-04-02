package com.supercilex.robotscouter.data.util;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public abstract class FirebaseTransformer implements ValueEventListener {
    protected final DatabaseReference mToRef;
    private Query mFromQuery;

    private TaskCompletionSource<DataSnapshot> mCompleteTask = new TaskCompletionSource<>();

    public FirebaseTransformer(DatabaseReference to) {
        mToRef = to;
    }

    public FirebaseTransformer(Query from, DatabaseReference to) {
        mFromQuery = from;
        mToRef = to;
    }

    protected abstract Task<Void> transform(DataSnapshot transformSnapshot);

    @CallSuper
    @Override
    public void onDataChange(final DataSnapshot snapshot) {
        transform(snapshot).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) mCompleteTask.setResult(snapshot);
                else mCompleteTask.setException(task.getException());
            }
        });
    }

    public void setFromQuery(Query from) {
        mFromQuery = from;
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
