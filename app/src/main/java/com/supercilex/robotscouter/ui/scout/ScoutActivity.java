package com.supercilex.robotscouter.ui.scout;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.client.DownloadTeamDataJob;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaApi;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.data.util.TeamIndices;
import com.supercilex.robotscouter.ui.TeamSender;
import com.supercilex.robotscouter.ui.common.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.scout.template.ScoutTemplateSheet;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;
import com.supercilex.robotscouter.util.MiscellaneousHelper;

import java.util.Collections;
import java.util.List;

public class ScoutActivity extends AppCompatActivity implements ValueEventListener {
    private static final String INTENT_ADD_SCOUT = "add_scout";

    private TeamHelper mTeamHelper;
    private AppBarViewHolder mHolder;
    private ScoutPagerAdapter mPagerAdapter;
    private boolean mInitScouting = true;
    private Bundle mSavedState;

    public static void start(Context context, TeamHelper teamHelper, boolean addScout) {
        Intent starter = teamHelper.getIntent().setClass(context, ScoutActivity.class);
        starter.putExtra(INTENT_ADD_SCOUT, addScout);

        starter.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            starter.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            starter.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }

        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scout);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mHolder = new AppBarViewHolder(this);
        mSavedState = savedInstanceState;

        mTeamHelper = TeamHelper.get(getIntent());
        mHolder.bind(mTeamHelper);
        addTeamAndScoutListeners();

        if (mSavedState == null && MiscellaneousHelper.isOffline(this)) {
            Snackbar.make(findViewById(R.id.root),
                          R.string.offline_reassurance,
                          Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUserActions.getInstance().start(mTeamHelper.getViewAction());
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUserActions.getInstance().end(mTeamHelper.getViewAction());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTeamHelper.getRef().removeEventListener(this);
        mPagerAdapter.cleanup();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putAll(ScoutUtils.getScoutKeyBundle(mPagerAdapter.getCurrentScoutKey()));
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scout, menu);
        mHolder.initMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_scout:
                mPagerAdapter.setCurrentScoutKey(ScoutUtils.add(mTeamHelper.getTeam()));
                break;
            case R.id.action_share:
                TeamSender.launchInvitationIntent(this, Collections.singletonList(mTeamHelper));
                break;
            case R.id.action_visit_tba_team_website:
                mTeamHelper.visitTbaWebsite(this);
                break;
            case R.id.action_visit_team_website:
                mTeamHelper.visitTeamWebsite(this);
                break;
            case R.id.action_edit_scout_templates:
                DownloadTeamDataJob.cancelAll(this);
                ScoutTemplateSheet.show(getSupportFragmentManager(), mTeamHelper);
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(getSupportFragmentManager(), mTeamHelper);
                break;
            case android.R.id.home:
                if (NavUtils.shouldUpRecreateTask(
                        this, new Intent(this, TeamListActivity.class))) {
                    TaskStackBuilder.create(this).addParentStack(this).startActivities();
                    finish();
                } else {
                    NavUtils.navigateUpFromSameTask(this);
                }
                break;
            default:
                return false;
        }
        return true;
    }

    private void addTeamAndScoutListeners() {
        if (TextUtils.isEmpty(mTeamHelper.getTeam().getKey())) {
            TeamIndices.getAll()
                    .addOnSuccessListener(this, new OnSuccessListener<List<DataSnapshot>>() {
                        @Override
                        public void onSuccess(List<DataSnapshot> snapshots) {
                            for (DataSnapshot keySnapshot : snapshots) {
                                if (keySnapshot.getValue()
                                        .equals(mTeamHelper.getTeam().getNumberAsLong())) {
                                    mTeamHelper.getTeam().setKey(keySnapshot.getKey());
                                    addTeamAndScoutListeners();
                                    return;
                                }
                            }

                            mTeamHelper.addTeam(ScoutActivity.this);
                            addTeamAndScoutListeners();
                            TbaApi.fetch(mTeamHelper.getTeam(), ScoutActivity.this)
                                    .addOnCompleteListener(
                                            ScoutActivity.this,
                                            new OnCompleteListener<Team>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Team> task) {
                                                    if (task.isSuccessful()) {
                                                        mTeamHelper.updateTeam(task.getResult());
                                                    } else {
                                                        DownloadTeamDataJob.start(ScoutActivity.this,
                                                                                  mTeamHelper);
                                                    }
                                                }
                                            });
                        }
                    });
        } else {
            mTeamHelper.getRef().addValueEventListener(this);
        }
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        if (snapshot.getValue() == null) {
            finish();
            return;
        }

        String key = mTeamHelper.getTeam().getKey();
        mTeamHelper = snapshot.getValue(Team.class).getHelper();
        mTeamHelper.getTeam().setKey(key);
        mHolder.bind(mTeamHelper);

        if (mInitScouting) {
            mInitScouting = false;

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
            String scoutKey = null;

            if (mSavedState != null) scoutKey = ScoutUtils.getScoutKey(mSavedState);
            mPagerAdapter = new ScoutPagerAdapter(this, tabLayout, mTeamHelper, scoutKey);

            viewPager.setAdapter(mPagerAdapter);
            tabLayout.setupWithViewPager(viewPager);


            if (getIntent().getBooleanExtra(INTENT_ADD_SCOUT, false)) {
                getIntent().removeExtra(INTENT_ADD_SCOUT);
                mPagerAdapter.setCurrentScoutKey(ScoutUtils.add(mTeamHelper.getTeam()));
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }
}
