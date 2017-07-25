package com.supercilex.robotscouter.ui.scout;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.ScoutUtilsKt;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.TeamHolder;
import com.supercilex.robotscouter.ui.TeamMediaCreator;
import com.supercilex.robotscouter.ui.TeamSharer;
import com.supercilex.robotscouter.ui.scout.template.ScoutTemplateSheet;

import java.util.Collections;

import static com.supercilex.robotscouter.data.client.DownloadTeamDataJobKt.cancelAllDownloadTeamDataJobs;
import static com.supercilex.robotscouter.data.util.ScoutUtilsKt.addScout;
import static com.supercilex.robotscouter.data.util.ScoutUtilsKt.getScoutKeyBundle;
import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.logEditTeamDetailsEvent;
import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.logEditTemplateEvent;
import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.logShareTeamEvent;
import static com.supercilex.robotscouter.util.ConnectivityUtilsKt.isOffline;

public abstract class ScoutListFragmentBase extends LifecycleFragment
        implements Observer<TeamHelper>, TeamMediaCreator.StartCaptureListener {
    public static final String KEY_SCOUT_ARGS = "scout_args";
    private static final String KEY_ADD_SCOUT = "add_scout";

    protected View mRootView;
    protected AppBarViewHolderBase mViewHolder;

    private TeamHolder mDataHolder;
    private TeamHelper mTeamHelper;
    private ScoutPagerAdapter mPagerAdapter;

    private TaskCompletionSource<Void> mOnScoutingReadyTask;
    private Bundle mSavedState;

    public static Bundle getBundle(Team team, boolean addScout, String scoutKey) {
        Bundle args = team.getHelper().toBundle();
        args.putBoolean(KEY_ADD_SCOUT, addScout);
        args.putAll(getScoutKeyBundle(scoutKey));
        return args;
    }

    protected static ScoutListFragmentBase setArgs(ScoutListFragmentBase fragment, Bundle args) {
        fragment.setArguments(args);
        return fragment;
    }

    protected Bundle getBundle() {
        return ScoutListFragmentBase.getBundle(
                mTeamHelper.getTeam(),
                getArguments().getBoolean(KEY_ADD_SCOUT),
                getScoutKey());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataHolder = ViewModelProviders.of(this).get(TeamHolder.class);
        mDataHolder.init(savedInstanceState == null ? getArguments() : savedInstanceState);
        mTeamHelper = mDataHolder.getTeamHelperListener().getValue();
        mDataHolder.getTeamHelperListener().observe(this, this);
        mOnScoutingReadyTask = new TaskCompletionSource<>();
    }

    @Override
    public void onChanged(@Nullable TeamHelper helper) {
        if (helper == null) onTeamDeleted();
        else {
            mTeamHelper = helper;
            if (!mOnScoutingReadyTask.getTask().isComplete()) {
                initScoutList();
                mOnScoutingReadyTask.setResult(null);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_scout_list, container, false);
        mSavedState = savedInstanceState;

        showOfflineReassurance();

        return mRootView;
    }

    private void showOfflineReassurance() {
        if (mSavedState == null && isOffline()) {
            Snackbar.make(mRootView,
                          R.string.offline_reassurance,
                          Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewHolder = newAppBarViewHolder(
                mDataHolder.getTeamHelperListener(), mOnScoutingReadyTask.getTask());
        if (savedInstanceState != null) mViewHolder.restoreState(savedInstanceState);
    }

    protected abstract AppBarViewHolderBase newAppBarViewHolder(LiveData<TeamHelper> listener,
                                                                Task<Void> onScoutingReadyTask);

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUserActions.getInstance().start(mTeamHelper.getViewAction());
    }

    @Override
    public void onStop() {
        super.onStop();
        FirebaseUserActions.getInstance().end(mTeamHelper.getViewAction());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPagerAdapter != null) mPagerAdapter.cleanup();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPagerAdapter != null) {
            outState.putAll(getScoutKeyBundle(mPagerAdapter.getCurrentScoutKey()));
        }
        mViewHolder.onSaveInstanceState(outState);
    }

    /**
     * Used in {@link TeamMediaCreator#startCapture(boolean)}
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mViewHolder.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStartCapture(boolean shouldUploadMediaToTba) {
        mViewHolder.onStartCapture(shouldUploadMediaToTba);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mViewHolder.onActivityResult(requestCode, resultCode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String teamNumber = mTeamHelper.getTeam().getNumber();
        switch (item.getItemId()) {
            case R.id.action_new_scout:
                mPagerAdapter.setCurrentScoutKey(addScout(mTeamHelper.getTeam()));
                break;
            case R.id.action_add_media:
                ShouldUploadMediaToTbaDialog.Companion.show(this);
                break;
            case R.id.action_share:
                TeamSharer.Companion.shareTeams(getActivity(),
                                                Collections.singletonList(mTeamHelper));
                logShareTeamEvent(teamNumber);
                break;
            case R.id.action_visit_tba_website:
                mTeamHelper.visitTbaWebsite(getContext());
                break;
            case R.id.action_visit_team_website:
                mTeamHelper.visitTeamWebsite(getContext());
                break;
            case R.id.action_edit_scout_templates:
                cancelAllDownloadTeamDataJobs(getContext());
                ScoutTemplateSheet.show(getChildFragmentManager(), mTeamHelper);
                logEditTemplateEvent(teamNumber);
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(getChildFragmentManager(), mTeamHelper);
                logEditTeamDetailsEvent(teamNumber);
                break;
            case R.id.action_delete:
                mPagerAdapter.onScoutDeleted();
                break;
            default:
                return false;
        }
        return true;
    }

    private void initScoutList() {
        TabLayout tabLayout = mRootView.findViewById(R.id.tabs);
        ViewPager viewPager = mRootView.findViewById(R.id.viewpager);
        mPagerAdapter = new ScoutPagerAdapter(this,
                                              mViewHolder,
                                              tabLayout,
                                              mTeamHelper,
                                              getScoutKey());

        viewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);


        if (getArguments().getBoolean(KEY_ADD_SCOUT, false)) {
            getArguments().remove(KEY_ADD_SCOUT);
            mPagerAdapter.setCurrentScoutKey(addScout(mTeamHelper.getTeam()));
        }
    }

    private String getScoutKey() {
        String scoutKey;

        if (mPagerAdapter != null) scoutKey = mPagerAdapter.getCurrentScoutKey(); // NOPMD
        else if (mSavedState != null) scoutKey = ScoutUtilsKt.getScoutKey(mSavedState); // NOPMD
        else scoutKey = ScoutUtilsKt.getScoutKey(getArguments());

        return scoutKey;
    }

    protected abstract void onTeamDeleted();
}
