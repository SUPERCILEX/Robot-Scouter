package com.supercilex.robotscouter.ui.teamlist;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;
import com.supercilex.robotscouter.util.AnalyticsUtils;
import com.supercilex.robotscouter.util.ConnectivityUtils;
import com.supercilex.robotscouter.util.PreferencesUtils;
import com.supercilex.robotscouter.util.RemoteConfigUtils;
import com.supercilex.robotscouter.util.ViewUtils;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase.KEY_SCOUT_ARGS;

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatActivity
        implements View.OnClickListener, Runnable, DialogInterface.OnCancelListener, TeamSelectionListener, OnSuccessListener<Void> {
    private static final int RC_SCOUT = 744;
    private static final int API_AVAILABILITY_RC = 65;
    private static final String MINIMUM_APP_VERSION_KEY = "minimum_app_version";

    private TeamListFragment mTeamListFragment;
    private AuthHelper mAuthHelper;
    private MaterialTapTargetPrompt mAddTeamPrompt;

    @Override
    @AddTrace(name = "onCreate")
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        ViewUtils.isTabletMode(this);

        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mTeamListFragment =
                (TeamListFragment) getSupportFragmentManager().findFragmentByTag(TeamListFragment.TAG);
        mAuthHelper = new AuthHelper(this);
        mAddTeamPrompt = TutorialHelper.showCreateFirstTeamPrompt(this);

        findViewById(R.id.fab).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PlayServicesHelper.makePlayServicesAvailable(this, API_AVAILABILITY_RC, this);
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        GoogleApiAvailability.getInstance().showErrorNotification(this, result);

        RemoteConfigUtils.fetchAndActivate().addOnSuccessListener(this, this);
    }

    @Override
    public void onSuccess(Void aVoid) {
        double minimum = FirebaseRemoteConfig.getInstance().getDouble(MINIMUM_APP_VERSION_KEY);
        if (BuildConfig.VERSION_CODE < minimum && !ConnectivityUtils.isOffline(this)) {
            UpdateDialog.Companion.show(getSupportFragmentManager());
        }
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
                DonateDialog.Companion.show(getSupportFragmentManager());
                break;
            case R.id.action_licenses:
                LicensesDialog.Companion.show(getSupportFragmentManager());
                break;
            case R.id.action_about:
                AboutDialog.Companion.show(getSupportFragmentManager());
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mAuthHelper.onActivityResult(requestCode, resultCode, data) && mAddTeamPrompt != null) {
            mAddTeamPrompt.dismiss();
            PreferencesUtils.setHasShownAddTeamTutorial(this, true);
            PreferencesUtils.setHasShownSignInTutorial(this, true);
        }
        if (requestCode == RC_SCOUT && resultCode == Activity.RESULT_OK) {
            onTeamSelected(data.getBundleExtra(KEY_SCOUT_ARGS), true);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mTeamListFragment.onBackPressed()) super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            if (AuthHelper.isSignedIn()) {
                NewTeamDialog.Companion.show(getSupportFragmentManager());
            } else {
                mAuthHelper.showSignInResolution();
            }
        }
    }

    @Override
    public void onTeamSelected(Bundle args, boolean restoreOnConfigChange) {
        Team team = TeamHelper.parse(args).getTeam();

        if (ViewUtils.isTabletMode(this)) {
            mTeamListFragment.selectTeam(null);
            mTeamListFragment.selectTeam(team);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.scouts, TabletScoutListFragment.newInstance(args))
                    .commit();
        } else {
            if (restoreOnConfigChange) {
                startActivityForResult(ScoutActivity.Companion.createIntent(this, args), RC_SCOUT);
            } else {
                startActivity(ScoutActivity.Companion.createIntent(this, args));
            }
            mTeamListFragment.selectTeam(null);
        }

        AnalyticsUtils.selectTeam(team.getNumber());
    }
}
