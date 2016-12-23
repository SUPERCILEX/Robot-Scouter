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

// TODO: 08/10/2016 add Firebase analytics to menu item clicks
// so I know what stuff to put on top

// TODO: 08/10/2016 If firebase bug isn't resolved,
// make users enter their team number to setup
// database with their team as example.

@SuppressLint("GoogleAppIndexingApiWarning")
public class TeamListActivity extends AppCompatBase implements View.OnClickListener {
    private AuthHelper mAuthHelper;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.RobotScouter_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        findViewById(R.id.fab).setOnClickListener(this);
        mAuthHelper = AuthHelper.init(this);
        DeepLinkHelper.init(this);
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
            case R.id.action_settings:
                break;
            default:
                return true;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            if (AuthHelper.isSignedIn()) {
                NewTeamDialog.show(getSupportFragmentManager());
            } else {
                mAuthHelper.startSignInResolution();
            }
        }
    }
}
