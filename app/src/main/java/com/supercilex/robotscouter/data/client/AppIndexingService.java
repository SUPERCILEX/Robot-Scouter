package com.supercilex.robotscouter.data.client;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.Builder;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.data.util.TeamIndices;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class AppIndexingService extends IntentService implements OnSuccessListener<List<TeamHelper>> {
    public AppIndexingService() {
        super("AppIndexingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int result = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(getApplicationContext());

        if (result == ConnectionResult.SUCCESS) {
            TeamRetriever.getAll().addOnSuccessListener(this);
        } else {
            GoogleApiAvailability.getInstance().showErrorNotification(this, result);
        }
    }

    @Override
    public void onSuccess(List<TeamHelper> teams) {
        ArrayList<Indexable> indexableTeams = new ArrayList<>();

        for (TeamHelper teamHelper : teams) {
            indexableTeams.add(teamHelper.getIndexable());
        }

        if (!indexableTeams.isEmpty()) {
            FirebaseAppIndex.getInstance()
                    .update(indexableTeams.toArray(new Indexable[indexableTeams.size()]));
        }
    }

    private static class TeamRetriever
            implements Builder<Task<List<TeamHelper>>>, OnSuccessListener<List<DataSnapshot>>, OnFailureListener {
        private TaskCompletionSource<List<TeamHelper>> mAllTeamsTask = new TaskCompletionSource<>();
        private List<TeamHelper> mTeamHelpers = new ArrayList<>();

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

        public static Task<List<TeamHelper>> getAll() {
            return new TeamRetriever().build();
        }

        @Override
        public Task<List<TeamHelper>> build() {
            return mAllTeamsTask.getTask();
        }

        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        @Override
        public void onSuccess(List<DataSnapshot> snapshots) {
            List<Task<Void>> teamTasks = new ArrayList<>();

            for (DataSnapshot teamIndexSnapshot : snapshots) {
                final TaskCompletionSource<Void> teamTask = new TaskCompletionSource<>();
                teamTasks.add(teamTask.getTask());

                Constants.FIREBASE_TEAMS_REF.child(teamIndexSnapshot.getKey())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.getValue() == null) {
                                    teamTask.setException(
                                            new IllegalArgumentException("Team was null: " + snapshot));
                                } else {
                                    mTeamHelpers.add(new Team.Builder(snapshot.getValue(Team.class))
                                                             .setKey(snapshot.getKey())
                                                             .build()
                                                             .getHelper());
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
                    mAllTeamsTask.setResult(mTeamHelpers);
                }
            }).addOnFailureListener(this);
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            FirebaseCrash.report(e);
            mAllTeamsTask.trySetException(e);
        }
    }
}
