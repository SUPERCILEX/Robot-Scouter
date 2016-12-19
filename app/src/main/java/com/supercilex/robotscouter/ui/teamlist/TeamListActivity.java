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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.User;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.LogFailureListener;

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
                                mMenu.findItem(R.id.action_sign_in).setVisible(true);
                                mMenu.findItem(R.id.action_sign_out).setVisible(false);
                                signInAnonymously();
                            }
                        })
                        .addOnFailureListener(new LogFailureListener());
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
            mMenu.findItem(R.id.action_sign_in).setVisible(false);
            mMenu.findItem(R.id.action_sign_out).setVisible(true);
            mHelper.showSnackbar(R.string.signed_in, Snackbar.LENGTH_LONG);

            FirebaseUser user = BaseHelper.getUser();
            new User.Builder(user.getUid())
                    .setEmail(user.getEmail())
                    .setName(user.getDisplayName())
                    .setPhotoUrl(user.getPhotoUrl())
                    .build()
                    .add();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                NewTeamDialog.show(getSupportFragmentManager());
                break;
        }
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
                .addOnFailureListener(new LogFailureListener())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mTeamsFragment.setAdapter();
                        } else {
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
                    }
                });
    }
}
