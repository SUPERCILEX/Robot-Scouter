package com.supercilex.robotscouter.ui.teamlist;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.util.PlayServicesHelper;
import com.google.android.gms.common.GoogleApiAvailability;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.AuthHelper;

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatActivity
        implements View.OnClickListener, Runnable, DialogInterface.OnCancelListener {
    private static final int API_AVAILABILITY_RC = 65;

    private AuthHelper mAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        findViewById(R.id.fab).setOnClickListener(this);

        mAuthHelper = AuthHelper.init(this);
        TutorialHelper.showCreateFirstTeamPrompt(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PlayServicesHelper.makePlayServicesAvailable(this, API_AVAILABILITY_RC, this);
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        GoogleApiAvailability.getInstance().showErrorNotification(this, result);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.team_list, menu);
        mAuthHelper.initMenu(menu);
        new Handler().post(this);
        return true;
    }

    @Override
    public void run() {
        TutorialHelper.showSignInPrompt(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                mAuthHelper.signIn();
                break;
            case R.id.action_sign_out:
                mAuthHelper.signOut();
                break;
            case R.id.action_donate:
                DonateDialog.show(getSupportFragmentManager());
                break;
            case R.id.action_licenses:
                LicensesDialog.show(getSupportFragmentManager());
                break;
            case R.id.action_about:
                AboutDialog.show(getSupportFragmentManager());
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        OnBackPressedListener listener = (OnBackPressedListener)
                getSupportFragmentManager().findFragmentByTag("team_list_fragment");
        if (!listener.onBackPressed()) super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            if (AuthHelper.isSignedIn()) {
                NewTeamDialog.show(getSupportFragmentManager());
            } else {
                mAuthHelper.showSignInResolution();
            }
        }
    }
}
