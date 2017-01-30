package com.supercilex.robotscouter.data.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TeamIndices implements Builder<Task<List<DataSnapshot>>>, ValueEventListener {
    private TaskCompletionSource<List<DataSnapshot>> mTeamIndicesTask = new TaskCompletionSource<>();
    private List<DataSnapshot> mTeamIndicesSnapshots = new ArrayList<>();

    protected TeamIndices() {
        TeamHelper.getIndicesRef().addListenerForSingleValueEvent(this);
    }

    public static Task<List<DataSnapshot>> getAll() {
        return new TeamIndices().build();
    }

    @Override
    public Task<List<DataSnapshot>> build() {
        return mTeamIndicesTask.getTask();
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        for (DataSnapshot indexSnapshot : snapshot.getChildren()) {
            mTeamIndicesSnapshots.add(indexSnapshot);
        }
        mTeamIndicesTask.setResult(mTeamIndicesSnapshots);
    }

    @Override
    public void onCancelled(DatabaseError error) {
        mTeamIndicesTask.setException(error.toException());
        FirebaseCrash.report(error.toException());
    }
}
