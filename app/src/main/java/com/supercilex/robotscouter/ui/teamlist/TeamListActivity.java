package com.supercilex.robotscouter.ui.teamlist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.User;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.TaskFailureLogger;

// TODO: 08/10/2016 add Firebase analytics to menu item clicks
// so I know what stuff to put on top

// TODO: 08/10/2016 If firebase bug isn't resolved,
// make users enter their team number to setup
// database with their team as example.

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatBase implements View.OnClickListener {
    private static final int RC_SIGN_IN = 100;
    private TeamsFragment mTeamsFragment;
    private Menu mMenu;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mTeamsFragment = (TeamsFragment) getSupportFragmentManager()
                .findFragmentByTag("team_list_fragment");
        findViewById(R.id.fab).setOnClickListener(this);

        if (!BaseHelper.isSignedIn()) signInAnonymously();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.team_list, menu);
        mMenu = menu;

        if (BaseHelper.isSignedIn() && !BaseHelper.getUser().isAnonymous()) {
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
                signIn();
                break;
            case R.id.action_sign_out:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mTeamsFragment.cleanup();
                                setMenuIsSignedIn(false);
                                signInAnonymously();
                            }
                        })
                        .addOnFailureListener(new TaskFailureLogger());
                break;
            case R.id.action_licenses:
                LicensesDialog.show(getSupportFragmentManager());
                break;
            case R.id.action_settings:
                break;
            default:
                return true;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK) {
            // user is signed in!
            mTeamsFragment.setAdapter();
            mHelper.showSnackbar(R.string.signed_in, Snackbar.LENGTH_LONG);
            setMenuIsSignedIn(true);

            FirebaseUser firebaseUser = BaseHelper.getUser();
            User user = new User.Builder(firebaseUser.getUid())
                    .setEmail(firebaseUser.getEmail())
                    .setName(firebaseUser.getDisplayName())
                    .setPhotoUrl(firebaseUser.getPhotoUrl())
                    .build();
            user.add();

            IdpResponse response = IdpResponse.fromResultIntent(intent);
            if (response != null) user.transferData(response.getPrevUid());
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) NewTeamDialog.show(getSupportFragmentManager());
    }

    private void setMenuIsSignedIn(boolean signedIn) {
        mMenu.findItem(R.id.action_sign_in).setVisible(!signedIn);
        mMenu.findItem(R.id.action_sign_out).setVisible(signedIn);
    }

    private void signIn() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setProviders(Constants.ALL_PROVIDERS)
                        .setTheme(R.style.RobotScouter)
                        .setLogo(R.drawable.launch_logo)
                        .setShouldLinkAccounts(true)
                        .setTosUrl("https://www.example.com")
                        .build(),
                RC_SIGN_IN);
    }

    private void signInAnonymously() {
        BaseHelper.getAuth()
                .signInAnonymously()
                .addOnFailureListener(new TaskFailureLogger())
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        mTeamsFragment.setAdapter();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mHelper.showSnackbar(R.string.sign_in_failed,
                                             Snackbar.LENGTH_LONG,
                                             R.string.sign_in,
                                             new View.OnClickListener() {
                                                 @Override
                                                 public void onClick(View v) {
                                                     signIn();
                                                 }
                                             });
                    }
                });
    }
}
