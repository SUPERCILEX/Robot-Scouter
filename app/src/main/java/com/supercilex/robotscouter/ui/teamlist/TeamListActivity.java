package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseUtils;
import com.supercilex.robotscouter.util.LogFailureListener;
import com.supercilex.robotscouter.util.TagUtils;

import java.util.Arrays;

import static com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN;

// TODO: 08/10/2016 add Firebase analytics to menu item clicks so I know what stuff to put on top
// TODO: 08/10/2016 make users enter their team number to setup database with their team as example. Also add Firebase analytics to make sure this isn't getting rid of users.
// TODO: 08/31/2016 Look for FirebaseCrash.report() and set up FirebaseCrash.log(). log will put logs for the crash.

public class TeamListActivity extends AppCompatActivity {
    private static final String MANAGER_STATE = "manager_state";
    private static final String COUNT = "count";

    private RecyclerView mTeams;
    private FirebaseRecyclerAdapter mAdapter;
    private LinearLayoutManager mManager;
    private Bundle mSavedState;
    private Menu mMenu;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mSavedState = savedInstanceState;

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new NewTeamDialogFragment().show(getSupportFragmentManager(),
                                                 TagUtils.getTag(this));
            }
        });

        mTeams = (RecyclerView) findViewById(R.id.team_list);
        mTeams.setHasFixedSize(true);
        mManager = new LinearLayoutManager(this);
        mTeams.setLayoutManager(mManager);
        // TODO: 09/03/2016 how to know when user is at bottom of RecyclerView for pagination

        if (FirebaseUtils.getUser() != null) {
            initAdapter();
        } else {
            signInAnonymously();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null) {
            outState.putParcelable(MANAGER_STATE, mManager.onSaveInstanceState());
            outState.putInt(COUNT, mAdapter.getItemCount());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.team_list, menu);
        mMenu = menu;

        if (FirebaseUtils.getUser() != null && !FirebaseUtils.getUser().isAnonymous()) {
            mMenu.findItem(R.id.action_sign_in).setVisible(false);
        } else {
            mMenu.findItem(R.id.action_sign_out).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                startActivityForResult(
                        AuthUI.getInstance().createSignInIntentBuilder()
                                .setLogo(R.drawable.launch_logo_image)
                                .setProviders(
                                        Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER)
                                                              .build(),
                                                      new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER)
                                                              .build(),
                                                      new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER)
                                                              .build(),
                                                      new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER)
                                                              .build()))
                                .build(),
                        RC_SIGN_IN);
                break;
            case R.id.action_sign_out:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mTeams.setAdapter(null);
                                mAdapter.cleanup();
                                mMenu.findItem(R.id.action_sign_in).setVisible(true);
                                mMenu.findItem(R.id.action_sign_out).setVisible(false);
                                signInAnonymously();
                            }
                        })
                        .addOnFailureListener(new LogFailureListener());
                break;
            case R.id.action_settings:
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // user is signed in!
                initAdapter();
                mMenu.findItem(R.id.action_sign_in).setVisible(false);
                mMenu.findItem(R.id.action_sign_out).setVisible(true);
                Snackbar.make(findViewById(android.R.id.content),
                              R.string.signed_in,
                              Snackbar.LENGTH_LONG).show();

                DatabaseReference ref = FirebaseUtils.getDatabase()
                        .child("users")
                        .child(FirebaseUtils.getUid());
                ref.child("name").setValue(FirebaseUtils.getUser().getDisplayName());
                ref.child("provider").setValue(FirebaseUtils.getUser().getProviderId());
                ref.child("email").setValue(FirebaseUtils.getUser().getEmail());
            }
        }
    }

    private void initAdapter() {
        mAdapter = new FirebaseIndexRecyclerAdapter<Team, TeamHolder>(
                Team.class,
                R.layout.activity_team_list_row_layout,
                TeamHolder.class,
                FirebaseUtils.getDatabase()
                        .child(Constants.FIREBASE_TEAM_INDEXES)
                        .child(FirebaseUtils.getUid()),
                FirebaseUtils.getDatabase().child(Constants.FIREBASE_TEAMS)) {
            @Override
            public void populateViewHolder(TeamHolder teamHolder, Team team, int position) {
                String teamNumber = team.getNumber();
                String key = getRef(position).getKey();

                teamHolder.setTeamNumber(teamNumber);
                teamHolder.setTeamName(team.getName(),
                                       TeamListActivity.this.getString(R.string.unknown_team));
                teamHolder.setTeamLogo(TeamListActivity.this, team.getMedia());
                teamHolder.setListItemClickListener(TeamListActivity.this, teamNumber, key);
                teamHolder.setCreateNewScoutListener(TeamListActivity.this, teamNumber, key);

                team.fetchLatestData(TeamListActivity.this, key);
            }

            @Override
            protected void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.report(databaseError.toException());
            }
        };

        mTeams.setAdapter(mAdapter);

        if (mSavedState != null) {
            mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    if (mAdapter.getItemCount() >= mSavedState.getInt(COUNT)) {
                        mManager.onRestoreInstanceState(mSavedState.getParcelable(MANAGER_STATE));
                        mAdapter.unregisterAdapterDataObserver(this);
                    }
                }
            });
        }
    }

    private void signInAnonymously() {
        FirebaseCrash.log("Attempting to sign in anonymously.");
        FirebaseUtils.getAuth()
                .signInAnonymously()
                .addOnCompleteListener(this, new AnonymousSignInListener())
                .addOnFailureListener(new LogFailureListener());
    }

    private class AnonymousSignInListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                initAdapter();
            } else {
                // TODO
            }
        }
    }
}
