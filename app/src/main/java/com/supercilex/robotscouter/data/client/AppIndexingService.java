package com.supercilex.robotscouter.data.client;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.TaskFailureLogger;

import java.util.ArrayList;
import java.util.List;

public class AppIndexingService extends IntentService implements OnSuccessListener<List<Team>> {
    public AppIndexingService() {
        super("AppIndexingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        TeamRetriever.getAll()
                .addOnSuccessListener(this)
                .addOnFailureListener(new TaskFailureLogger());
    }

    @Override
    public void onSuccess(List<Team> teams) {
        ArrayList<Indexable> indexableTeams = new ArrayList<>();

        for (Team team : teams) {
            indexableTeams.add(team.getIndexable());
        }

        if (indexableTeams.size() > 0) {
            FirebaseAppIndex.getInstance().update(indexableTeams.toArray(new Indexable[0]));
        }
    }

    private static class TeamRetriever implements ValueEventListener, OnSuccessListener<Void>, OnFailureListener {
        private TaskCompletionSource<List<Team>> mAllTeamsTask = new TaskCompletionSource<>();
        private List<Team> mTeams = new ArrayList<>();

        private TeamRetriever() {
            Team.getIndicesRef().addListenerForSingleValueEvent(this);
        }

        public static Task<List<Team>> getAll() {
            return new TeamRetriever().getTask();
        }

        private Task<List<Team>> getTask() {
            return mAllTeamsTask.getTask();
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            List<Task<Void>> teamTasks = new ArrayList<>();

            for (DataSnapshot teamIndexSnapshot : snapshot.getChildren()) {
                final TaskCompletionSource<Void> teamTask = new TaskCompletionSource<>();
                teamTasks.add(teamTask.getTask());

                Constants.FIREBASE_TEAMS.child(teamIndexSnapshot.getKey())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.getValue() == null) {
                                    teamTask.setException(
                                            new IllegalArgumentException("Team was null"));
                                } else {
                                    mTeams.add(new Team.Builder(snapshot.getValue(Team.class))
                                                       .setKey(snapshot.getKey())
                                                       .build());
                                    teamTask.setResult(null);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                teamTask.setException(error.toException());
                                TeamRetriever.this.onCancelled(error);
                            }
                        });
            }

            Tasks.whenAll(teamTasks).addOnSuccessListener(this).addOnFailureListener(this);
        }

        @Override
        public void onSuccess(Void aVoid) {
            mAllTeamsTask.setResult(mTeams);
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            mAllTeamsTask.setException(e);
        }

        @Override
        public void onCancelled(DatabaseError error) {
            mAllTeamsTask.setException(error.toException());
            FirebaseCrash.report(error.toException());
        }
    }
}