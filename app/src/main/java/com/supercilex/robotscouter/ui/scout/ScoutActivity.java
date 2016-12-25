package com.supercilex.robotscouter.ui.scout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaApi;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.ui.scout.template.ScoutTemplatesSheet;
import com.supercilex.robotscouter.ui.teamlist.DeepLinkBuilder;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class ScoutActivity extends AppCompatBase implements ValueEventListener {
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
        addTeamListener();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        mPagerAdapter = new ScoutPagerAdapter(getSupportFragmentManager(),
                                              tabLayout,
                                              mTeam.getNumber());
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        if (savedInstanceState != null) {
            mPagerAdapter.setSavedTabKey(savedInstanceState.getString(Constants.SCOUT_KEY));
        }

        if (savedInstanceState == null && mHelper.isOffline()) {
            mHelper.showSnackbar(R.string.no_connection, Snackbar.LENGTH_SHORT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTeam.getRef().removeEventListener(this);
        mPagerAdapter.cleanup();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(Constants.SCOUT_KEY, mPagerAdapter.getSelectedTabKey());
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
                Scout.add(mTeam);
                mPagerAdapter.setManuallyAddedScout();
                break;
            case R.id.action_share:
                DeepLinkBuilder.launchInvitationIntent(this, mTeam);
                break;
            case R.id.action_edit_scout_templates:
                ScoutTemplatesSheet.show(getSupportFragmentManager(), mTeam);
                break;
            case R.id.action_visit_tba_team_website:
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(this, R.color.color_primary));
                builder.setShowTitle(true);
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this,
                                           Uri.parse("https://www.thebluealliance.com/team/"
                                                             + mTeam.getNumber()));
                break;
            case R.id.action_visit_team_website:
                CustomTabsIntent.Builder teamWebsiteBuilder = new CustomTabsIntent.Builder();
                teamWebsiteBuilder.setToolbarColor(ContextCompat.getColor(this,
                                                                          R.color.color_primary));
                teamWebsiteBuilder.setShowTitle(true);
                CustomTabsIntent teamWebsiteCustomTabsIntent = teamWebsiteBuilder.build();
                teamWebsiteCustomTabsIntent.launchUrl(this, Uri.parse(mTeam.getWebsite()));
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

    private void addTeamListener() {
        if (mTeam.getKey() == null) {
            Team.getIndicesRef().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if (child.getValue().toString().equals(mTeam.getNumber())) {
                                mTeam.setKey(child.getKey());
                                addTeamListener();
                                return;
                            }
                        }
                    }

                    mTeam.add();
                    addTeamListener();
                    TbaApi.fetch(mTeam, ScoutActivity.this)
                            .addOnCompleteListener(new OnCompleteListener<Team>() {
                                @Override
                                public void onComplete(@NonNull Task<Team> task) {
                                    if (task.isSuccessful()) {
                                        mTeam.update(task.getResult());
                                        BaseHelper.getDispatcher()
                                                .cancel(mTeam.getNumber());
                                    } else {
                                        mTeam.fetchLatestData();
                                    }
                                }
                            });
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    FirebaseCrash.report(error.toException());
                }
            });
        } else {
            mTeam.getRef().addValueEventListener(this);
            if (shouldAddNewScout()) {
                Scout.add(mTeam);
                getIntent().putExtra(INTENT_ADD_SCOUT, false);
            }
        }
    }

    private boolean shouldAddNewScout() {
        return getIntent().getBooleanExtra(INTENT_ADD_SCOUT, false);
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
