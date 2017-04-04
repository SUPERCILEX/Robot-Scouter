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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;
import com.supercilex.robotscouter.util.AnalyticsHelper;
import com.supercilex.robotscouter.util.RemoteConfigHelper;
import com.supercilex.robotscouter.util.ViewHelper;

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatActivity
        implements View.OnClickListener, Runnable, DialogInterface.OnCancelListener, TeamSelectionListener, OnSuccessListener<Void> {
    private static final int API_AVAILABILITY_RC = 65;
    private static final String SELECTED_TEAM_KEY = "selected_team_key";
    private static final String MINIMUM_APP_VERSION_KEY = "minimum_app_version";

    private TeamListFragment mTeamListFragment;
    private String mSelectedTeamKey;
    private AuthHelper mAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mTeamListFragment =
                (TeamListFragment) getSupportFragmentManager().findFragmentByTag(TeamListFragment.TAG);
        if (savedInstanceState != null && ViewHelper.isTabletMode(this)) {
            mSelectedTeamKey = savedInstanceState.getString(SELECTED_TEAM_KEY);
            mTeamListFragment.selectTeam(mSelectedTeamKey);
        }
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

        RemoteConfigHelper.fetchAndActivate().addOnSuccessListener(this, this);
    }

    @Override
    public void onSuccess(Void aVoid) {
        double minimum = FirebaseRemoteConfig.getInstance().getDouble(MINIMUM_APP_VERSION_KEY);
        if (BuildConfig.VERSION_CODE < minimum) UpdateDialog.show(getSupportFragmentManager());
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SELECTED_TEAM_KEY, mSelectedTeamKey);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (!mTeamListFragment.onBackPressed()) super.onBackPressed();
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

    @Override
    public void onTeamSelected(Team team, boolean addScout) {
        TeamHelper helper = team.getHelper();
        if (ViewHelper.isTabletMode(this)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.scouts, TabletScoutListFragment.newInstance(helper, addScout))
                    .commit();
        } else {
            ScoutActivity.start(this, helper, addScout);
        }
        AnalyticsHelper.selectTeam(team.getNumber());
    }

    @Override
    public void saveSelection(Team team) {
        mSelectedTeamKey = team == null ? null : team.getKey();
        mTeamListFragment.selectTeam(mSelectedTeamKey);
    }
}
