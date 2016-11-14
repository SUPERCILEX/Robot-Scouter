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
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.LogFailureListener;

import java.util.Arrays;

import static com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN;

// NOPMD TODO: 08/10/2016 add Firebase analytics to menu item clicks so I know what stuff to put on top
// NOPMD TODO: 08/10/2016 make users enter their team number to setup database with their team as example. Also add Firebase analytics to make sure this isn't getting rid of users.

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatBase {
    private TeamsFragment mTeamsFragment;
    private Menu mMenu;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new NewTeamDialog().show(getSupportFragmentManager(), mHelper.getTag());
            }
        });

        mTeamsFragment = ((TeamsFragment) getSupportFragmentManager().findFragmentByTag(
                "team_list_fragment"));
        if (!BaseHelper.isSignedIn()) {
            signInAnonymously();
        }
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
                new LicensesDialog().show(getSupportFragmentManager(), mHelper.getTag());
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

            DatabaseReference ref = BaseHelper.getDatabase()
                    .child("users")
                    .child(BaseHelper.getUid());
            ref.child("name").setValue(BaseHelper.getUser().getDisplayName());
            ref.child("provider").setValue(BaseHelper.getUser().getProviderId());
            ref.child("email").setValue(BaseHelper.getUser().getEmail());
        }
    }

    private void signIn() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setLogo(R.drawable.launch_logo)
                        .setProviders(
                                Arrays.asList(new AuthUI.IdpConfig
                                                      .Builder(AuthUI.EMAIL_PROVIDER).build(),
                                              new AuthUI.IdpConfig
                                                      .Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                              new AuthUI.IdpConfig
                                                      .Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                              new AuthUI.IdpConfig
                                                      .Builder(AuthUI.TWITTER_PROVIDER).build()))
                        .build(),
                RC_SIGN_IN);
    }

    private void signInAnonymously() {
        FirebaseCrash.log("Attempting to sign in anonymously.");
        BaseHelper.getAuth()
                .signInAnonymously()
                .addOnCompleteListener(this, new AnonymousSignInListener())
                .addOnFailureListener(new LogFailureListener());
    }

    private class AnonymousSignInListener implements OnCompleteListener<AuthResult> {
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
                                         public void onClick(View view) {
                                             signIn();
                                         }
                                     });
            }
        }
    }
}
