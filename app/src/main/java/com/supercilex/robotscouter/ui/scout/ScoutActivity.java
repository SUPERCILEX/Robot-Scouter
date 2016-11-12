package com.supercilex.robotscouter.ui.scout;

import android.app.ActivityManager;
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
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaService;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class ScoutActivity extends AppCompatBase implements ValueEventListener, ChildEventListener {
    private Team mTeam;
    private Menu mMenu;
    private ScoutPagerAdapter mPagerAdapter;
    // TODO: 11/09/2016 move this to getScoutRef in Scout.java
    private DatabaseReference mScoutRef;

    public static Intent createIntent(Context context, Team team) {
        Intent intent = BaseHelper.getTeamIntent(team).setClass(context, ScoutActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_scout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTeam = mHelper.getTeam();
        updateUi();
        addTeamListener();

        mPagerAdapter = new ScoutPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        // TODO: 11/10/2016 this is the to keep in activity, all else goes to fragment
        viewPager.setAdapter(mPagerAdapter);
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(viewPager);
        mScoutRef = BaseHelper.getDatabase()
                .child(Constants.FIREBASE_SCOUT_INDEXES)
                .child(BaseHelper.getUid())
                .child(mTeam.getNumber());
        mScoutRef.addChildEventListener(this);

        if (savedInstanceState == null && !BaseHelper.isNetworkAvailable(this)) {
            Snackbar.make(findViewById(android.R.id.content),
                          R.string.no_connection,
                          Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTeam.getTeamRef().removeEventListener((ValueEventListener) this);
        mScoutRef.removeEventListener((ChildEventListener) this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scout, menu);
        mMenu = menu;

        menu.findItem(R.id.action_visit_tba_team_website)
                .setTitle(String.format(getString(R.string.menu_item_visit_team_website_on_tba),
                                        mTeam.getNumber()));
        menu.findItem(R.id.action_visit_team_website)
                .setTitle(String.format(getString(R.string.menu_item_visit_team_website),
                                        mTeam.getNumber()));
        if (mTeam.getWebsite() != null) {
            mMenu.findItem(R.id.action_visit_team_website).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_scout:
                new Scout().createScoutId(mTeam.getNumber());
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
            case R.id.action_edit_details:
                EditDetailsDialog.newInstance(mTeam)
                        .show(getSupportFragmentManager(), mHelper.getTag());
                break;
            case R.id.action_settings:
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
        }

        return true;
    }

    private void updateUi() {
        if (mTeam.getName() != null) {
            String title = mTeam.getNumber() + " - " + mTeam.getName();
            setActivityTitle(title);
        } else {
            setActivityTitle(mTeam.getNumber());
        }

        Glide.with(ScoutActivity.this)
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_android_black_24dp)
                .into((ImageView) findViewById(R.id.backdrop));

        if (mMenu != null) {
            if (mTeam.getWebsite() != null) {
                mMenu.findItem(R.id.action_visit_team_website).setVisible(true);
            } else {
                mMenu.findItem(R.id.action_visit_team_website).setVisible(false);
            }
        }
    }

    private void setActivityTitle(String title) {
        getSupportActionBar().setTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(title));
        }
    }

    private void addTeamListener() {
        if (mTeam.getKey() != null) {
            mTeam.getTeamRef().addValueEventListener(this);
        } else {
            BaseHelper.getDatabase()
                    .child(Constants.FIREBASE_TEAM_INDEXES)
                    .child(BaseHelper.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                for (DataSnapshot child : dataSnapshot.getChildren()) {
                                    if (child.getValue().toString().equals(mTeam.getNumber())) {
                                        mTeam.setKey(child.getKey());
                                        addTeamListener();
                                        return;
                                    }
                                }
                            }

                            mTeam.add();
                            addTeamListener();
                            TbaService.start(mTeam, ScoutActivity.this)
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
                        public void onCancelled(DatabaseError databaseError) {
                            FirebaseCrash.report(databaseError.toException());
                        }
                    });
        }
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot.getValue() != null) {
            String key = mTeam.getKey();
            mTeam = dataSnapshot.getValue(Team.class);
            mTeam.setKey(key);
            updateUi();
        }
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        mPagerAdapter.add(dataSnapshot.getKey());
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        mPagerAdapter.remove(dataSnapshot.getKey());
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        FirebaseCrash.report(databaseError.toException());
    }
}
