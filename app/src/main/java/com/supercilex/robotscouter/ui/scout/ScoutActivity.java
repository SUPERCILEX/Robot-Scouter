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
import com.supercilex.robotscouter.data.TeamIndices;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaApi;
import com.supercilex.robotscouter.ui.TeamSender;
import com.supercilex.robotscouter.ui.common.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.scout.template.ScoutTemplatesSheet;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;
import com.supercilex.robotscouter.util.BaseHelper;

import java.util.Collections;
import java.util.List;

public class ScoutActivity extends AppCompatActivity implements ValueEventListener {
    private static final String INTENT_ADD_SCOUT = "add_scout";

    private Team mTeam;
    private AppBarViewHolder mHolder;
    private ScoutPagerAdapter mPagerAdapter;

    public static void start(Context context, Team team, boolean addScout) {
        Intent starter = team.getIntent().setClass(context, ScoutActivity.class);
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

        mTeam = Team.getTeam(getIntent());
        mHolder.bind(mTeam);
        addTeamAndScoutListeners(savedInstanceState);

        if (savedInstanceState == null && BaseHelper.isOffline(this)) {
            BaseHelper.showSnackbar(findViewById(R.id.root),
                                    R.string.offline_reassurance,
                                    Snackbar.LENGTH_LONG);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUserActions.getInstance().start(mTeam.getViewAction());
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUserActions.getInstance().end(mTeam.getViewAction());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTeam.getRef().removeEventListener(this);
        mPagerAdapter.cleanup();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putAll(Scout.getScoutKeyBundle(mPagerAdapter.getCurrentScoutKey()));
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
                mPagerAdapter.setCurrentScoutKey(Scout.add(mTeam));
                break;
            case R.id.action_share:
                TeamSender.launchInvitationIntent(this, Collections.singletonList(mTeam));
                break;
            case R.id.action_visit_tba_team_website:
                mTeam.visitTbaWebsite(this);
                break;
            case R.id.action_visit_team_website:
                mTeam.visitTeamWebsite(this);
                break;
            case R.id.action_edit_scout_templates:
                ScoutTemplatesSheet.show(getSupportFragmentManager(), mTeam);
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(mTeam, getSupportFragmentManager());
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

    private void addTeamAndScoutListeners(final Bundle savedInstanceState) {
        if (TextUtils.isEmpty(mTeam.getKey())) {
            TeamIndices.getAll()
                    .addOnSuccessListener(this, new OnSuccessListener<List<DataSnapshot>>() {
                        @Override
                        public void onSuccess(List<DataSnapshot> snapshots) {
                            for (DataSnapshot keySnapshot : snapshots) {
                                if (keySnapshot.getValue().equals(mTeam.getNumberAsLong())) {
                                    mTeam.setKey(keySnapshot.getKey());
                                    addTeamAndScoutListeners(savedInstanceState);
                                    return;
                                }
                            }

                            mTeam.add();
                            addTeamAndScoutListeners(savedInstanceState);
                            TbaApi.fetch(mTeam, ScoutActivity.this)
                                    .addOnCompleteListener(
                                            ScoutActivity.this,
                                            new OnCompleteListener<Team>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Team> task) {
                                                    if (task.isSuccessful()) {
                                                        mTeam.update(task.getResult());
                                                        BaseHelper.getDispatcher()
                                                                .cancel(mTeam.getNumber());
                                                    } else {
                                                        mTeam.fetchLatestData(ScoutActivity.this);
                                                    }
                                                }
                                            });
                        }
                    });
        } else {
            mTeam.getRef().addValueEventListener(this);

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
            String scoutKey = null;
            if (savedInstanceState != null) {
                scoutKey = Scout.getScoutKey(savedInstanceState);
            }
            mPagerAdapter = new ScoutPagerAdapter(getSupportFragmentManager(),
                                                  tabLayout,
                                                  mTeam.getKey(),
                                                  scoutKey);
            viewPager.setAdapter(mPagerAdapter);
            tabLayout.setupWithViewPager(viewPager);

            if (getIntent().getBooleanExtra(INTENT_ADD_SCOUT, false)) {
                mPagerAdapter.setCurrentScoutKey(Scout.add(mTeam));
                getIntent().removeExtra(INTENT_ADD_SCOUT);
            }
        }
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        if (snapshot.getValue() == null) {
            finish();
            return;
        }

        String key = mTeam.getKey();
        mTeam = snapshot.getValue(Team.class);
        mTeam.setKey(key);
        mHolder.bind(mTeam);
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }
}
