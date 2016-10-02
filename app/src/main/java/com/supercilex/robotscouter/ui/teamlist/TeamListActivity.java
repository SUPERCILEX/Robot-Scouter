package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.supercilex.robotscouter.dialogfragments.CreateNewTeamDialogFragment;
import com.supercilex.robotscouter.models.team.Team;
import com.supercilex.robotscouter.models.team.TeamHolder;

import static com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN;

// TODO: 06/21/2016 Add sign out flow
// TODO: 08/10/2016 add Firebase analytics to menu item clicks so I know what stuff to put on top
// TODO: 08/10/2016 make users enter their team number to setup database with their team as example. Also add Firebase analytics to make sure this isn't getting rid of users.
// TODO: 08/31/2016 Look for FirebaseCrash.report() and set up FirebaseCrash.log(). log will put logs for the crash.

public class TeamListActivity extends AppCompatActivity {
    private static final String MANAGER_STATE = "manager_state";
    private static final String COUNT = "count";

    private FirebaseRecyclerAdapter mAdapter;
    private RecyclerView mTeams;
    private LinearLayoutManager mManager;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseUser mFirebaseUser;

    private Bundle mSavedState;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mSavedState = savedInstanceState;
        mAuth = FirebaseAuth.getInstance();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new NewTeamDialogFragment().show(getSupportFragmentManager(), "newScout");
            }
        });

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mFirebaseUser = firebaseAuth.getCurrentUser();
                if (mFirebaseUser != null) {
                    attachRecyclerViewAdapter();
                } else {
                    mAuth.signInAnonymously()
                            .addOnFailureListener(MainActivity.this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // TODO: 09/24/2016 retry
                                    Snackbar.make(findViewById(android.R.id.content),
                                                  "Sign In Failed",
                                                  Snackbar.LENGTH_SHORT)
                                            .show();
                                    FirebaseCrash.report(e);
                                }
                            });
                    // TODO show a tutorial (pretend first time app start)
                }
            }
        };
        mAuth.addAuthStateListener(mAuthStateListener);

//        LeakCanary.install(getApplication());

        mTeams = (RecyclerView) findViewById(R.id.content_main_recycler_view);
        mTeams.setHasFixedSize(true);
        mManager = new LinearLayoutManager(this);
        mTeams.setLayoutManager(mManager);
        // TODO: 09/03/2016 how to know when user is at bottom of RecyclerView for pagination
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
        if (mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }

        if (mAdapter != null) {
            mAdapter.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
//        if (mDrawer.isDrawerOpen()) {
//            mDrawer.closeDrawer();
//        } else {
            super.onBackPressed();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
//            AuthUI.getInstance().signOut(MainActivity.this);
            // TODO: 09/06/2016 Fix sign in
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setLogo(R.drawable.launch_logo_image)
                            .setProviders(AuthUI.EMAIL_PROVIDER,
                                          AuthUI.GOOGLE_PROVIDER,
                                          AuthUI.FACEBOOK_PROVIDER)
                            .setTosUrl("example.com")
                            .build(),
                    RC_SIGN_IN);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // user is signed in!
                CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_activity_layout);
                Snackbar.make(coordinatorLayout,
                              R.string.successfully_signed_in,
                              Snackbar.LENGTH_LONG).show();

                FirebaseDatabase database = FirebaseUtils.getDatabase();
                mFirebaseUser = FirebaseUtils.getUser();
                DatabaseReference myRef = database.getReference()
                        .child("users")
                        .child(mFirebaseUser.getUid());
                myRef.child("uid").setValue(mFirebaseUser.getUid());
                myRef.child("name").setValue(mFirebaseUser.getDisplayName());
                myRef.child("provider").setValue(mFirebaseUser.getProviderId());
                myRef.child("email").setValue(mFirebaseUser.getEmail());
            }
        }
    }

    private void attachRecyclerViewAdapter() {
        mAdapter = new FirebaseIndexRecyclerAdapter<Team, TeamHolder>(
                Team.class,
                R.layout.activity_main_row_layout,
                TeamHolder.class,
                FirebaseUtils.getDatabase()
                        .getReference()
                        .child(Constants.FIREBASE_TEAM_INDEXES)
                        .child(mFirebaseUser.getUid())
                        .orderByValue(),
                FirebaseUtils.getDatabase().getReference().child(Constants.FIREBASE_TEAMS)) {
            @Override
            public void populateViewHolder(TeamHolder teamHolder, Team team, int position) {
                String teamNumber = team.getNumber();
                String key = getRef(position).getKey();

                teamHolder.setTeamNumber(teamNumber);
                teamHolder.setTeamName(team.getName(),
                                       TeamListActivity.this.getString(R.string.no_name));
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
                    super.onItemRangeInserted(positionStart, itemCount);

                    if (mAdapter.getItemCount() >= mSavedState.getInt(COUNT)) {
                        mManager.onRestoreInstanceState(mSavedState.getParcelable(MANAGER_STATE));
                        mAdapter.unregisterAdapterDataObserver(this);
                    }
                }
            });
        }
    }
}
