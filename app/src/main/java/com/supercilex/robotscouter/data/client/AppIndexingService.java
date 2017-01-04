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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.data.TeamIndices;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.Builder;
import com.supercilex.robotscouter.ui.AuthHelper;
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

        if (!indexableTeams.isEmpty()) {
            FirebaseAppIndex.getInstance()
                    .update(indexableTeams.toArray(new Indexable[indexableTeams.size()]));
        }
    }

    private static class TeamRetriever
            implements Builder<Task<List<Team>>>, OnSuccessListener<List<DataSnapshot>>, OnFailureListener {
        private TaskCompletionSource<List<Team>> mAllTeamsTask = new TaskCompletionSource<>();
        private List<Team> mTeams = new ArrayList<>();

        private TeamRetriever() {
            AuthHelper.onSignedIn(new OnSuccessListener<FirebaseAuth>() {
                @Override
                public void onSuccess(FirebaseAuth result) {
                    TeamIndices.getAll()
                            .addOnSuccessListener(TeamRetriever.this)
                            .addOnFailureListener(TeamRetriever.this);
                }
            });
        }

        public static Task<List<Team>> getAll() {
            return new TeamRetriever().build();
        }

        @Override
        public Task<List<Team>> build() {
            return mAllTeamsTask.getTask();
        }

        @Override
        public void onSuccess(List<DataSnapshot> snapshots) {
            List<Task<Void>> teamTasks = new ArrayList<>();

            for (DataSnapshot teamIndexSnapshot : snapshots) {
                final TaskCompletionSource<Void> teamTask = new TaskCompletionSource<>(); // NOPMD
                teamTasks.add(teamTask.getTask());

                Constants.FIREBASE_TEAMS.child(teamIndexSnapshot.getKey())
                        .addListenerForSingleValueEvent(new ValueEventListener() { // NOPMD
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.getValue() == null) {
                                    teamTask.setException(
                                            new IllegalArgumentException("Team was null: " + snapshot));
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
                                FirebaseCrash.report(error.toException());
                            }
                        });
            }

            Tasks.whenAll(teamTasks).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    mAllTeamsTask.setResult(mTeams);
                }
            }).addOnFailureListener(this);
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            mAllTeamsTask.setException(e);
        }
    }
}
