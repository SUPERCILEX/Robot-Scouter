package com.supercilex.robotscouter.ui.scout;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaService;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class ScoutActivity extends AppCompatBase
        implements ValueEventListener, Palette.PaletteAsyncListener {
    private Team mTeam;
    private Menu mMenu;
    private ScoutPagerAdapter mPagerAdapter;

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

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        mPagerAdapter = new ScoutPagerAdapter(getSupportFragmentManager(),
                                              tabLayout,
                                              Scout.getIndicesRef().child(mTeam.getNumber()));
        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        if (savedInstanceState != null) {
            mPagerAdapter.setSavedTabKey(savedInstanceState.getString(Constants.SCOUT_KEY));
        }

        if (savedInstanceState == null && !mHelper.isNetworkAvailable()) {
            Snackbar.make(findViewById(android.R.id.content),
                          R.string.no_connection,
                          Snackbar.LENGTH_SHORT).show();
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
        outState.putString(Constants.SCOUT_KEY,
                           mPagerAdapter.getSelectedTabKey());
        super.onSaveInstanceState(outState);
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
                mPagerAdapter.setManuallyAddedScout();
                break;
            case R.id.action_edit_scout_template:
                EditScoutTemplates.show(getSupportFragmentManager());
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
                EditDetailsDialog.show(mTeam, getSupportFragmentManager());
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
            default:
                return true;
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

        Glide.with(this)
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_android_black_24dp)
                .into((ImageView) findViewById(R.id.backdrop));

        Glide.with(this)
                .load(mTeam.getMedia())
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation glideAnimation) {
                        if (bitmap != null && !bitmap.isRecycled()) {
                            Palette.from(bitmap).generate(ScoutActivity.this);
                        }
                    }
                });


        if (mMenu != null) {
            if (mTeam.getWebsite() != null) {
                mMenu.findItem(R.id.action_visit_team_website).setVisible(true);
            } else {
                mMenu.findItem(R.id.action_visit_team_website).setVisible(false);
            }
        }
    }

    @Override
    public void onGenerated(Palette palette) {
        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
        CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.header);
        if (vibrantSwatch != null) {
            int opaque = vibrantSwatch.getRgb();
            toolbarLayout.setContentScrimColor(getTransparentColor(opaque));
            toolbarLayout.setStatusBarScrimColor(opaque);
        }
    }

    private int getTransparentColor(int opaque) {
        return Color.argb(Math.round(Color.alpha(opaque) * 0.6f),
                          Color.red(opaque),
                          Color.green(opaque),
                          Color.blue(opaque));
    }

    private void setActivityTitle(String title) {
        getSupportActionBar().setTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTaskDescription(new ActivityManager.TaskDescription(title));
        }
    }

    private void addTeamListener() {
        if (mTeam.getKey() != null) {
            mTeam.getRef().addValueEventListener(this);
        } else {
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
                    TbaService.fetch(mTeam, ScoutActivity.this)
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
        }
    }

    @Override
    public void onDataChange(DataSnapshot snapshot) {
        if (snapshot.getValue() != null) {
            String key = mTeam.getKey();
            mTeam = snapshot.getValue(Team.class);
            mTeam.setKey(key);
            updateUi();
        }
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }
}
