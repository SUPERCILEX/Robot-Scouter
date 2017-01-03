package com.supercilex.robotscouter.ui.teamlist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.AppCompatBase;

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatBase implements View.OnClickListener {
    private AuthHelper mAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        findViewById(R.id.fab).setOnClickListener(this);
        mAuthHelper = AuthHelper.init(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.team_list, menu);
        mAuthHelper.initMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_in:
                mAuthHelper.signIn();
                break;
            case R.id.action_sign_out:
                mAuthHelper.signOut();
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
        TeamListFragment teamListFragment = (TeamListFragment)
                getSupportFragmentManager().findFragmentByTag("team_list_fragment");
        if (!teamListFragment.onBackPressed()) super.onBackPressed();
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
