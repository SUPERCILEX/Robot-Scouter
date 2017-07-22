package com.supercilex.robotscouter.ui.scout;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaDownloader;
import com.supercilex.robotscouter.data.util.ScoutUtilsKt;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
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

public abstract class ScoutListFragmentBase extends Fragment
        implements ChangeEventListener, FirebaseAuth.AuthStateListener, TeamMediaCreator.StartCaptureListener {
    public static final String KEY_SCOUT_ARGS = "scout_args";
    private static final String KEY_ADD_SCOUT = "add_scout";

    protected View mRootView;
    protected AppBarViewHolderBase mHolder;

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
        mTeamHelper = TeamHelper.parse(getArguments());
        mOnScoutingReadyTask = new TaskCompletionSource<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_scout_list, container, false);
        mSavedState = savedInstanceState;

        FirebaseAuth.getInstance().addAuthStateListener(this);
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
        mHolder = newAppBarViewHolder(mTeamHelper, mOnScoutingReadyTask.getTask());
        if (savedInstanceState != null) mHolder.restoreState(savedInstanceState);
        mHolder.bind(mTeamHelper);
    }

    protected abstract AppBarViewHolderBase newAppBarViewHolder(TeamHelper teamHelper,
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
        FirebaseAuth.getInstance().removeAuthStateListener(this);
        removeListeners();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPagerAdapter != null) {
            outState.putAll(getScoutKeyBundle(mPagerAdapter.getCurrentScoutKey()));
        }
        mHolder.onSaveInstanceState(outState);
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
        mHolder.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStartCapture(boolean shouldUploadMediaToTba) {
        mHolder.onStartCapture(shouldUploadMediaToTba);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mHolder.onActivityResult(requestCode, resultCode);
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

    private void addListeners() {
        if (TextUtils.isEmpty(mTeamHelper.getTeam().getKey())) {
            for (int i = 0; i < Constants.sFirebaseTeams.size(); i++) {
                Team team = Constants.sFirebaseTeams.getObject(i);
                if (team.getNumberAsLong() == mTeamHelper.getTeam().getNumberAsLong()) {
                    mTeamHelper.getTeam().setKey(team.getKey());
                    addListeners();
                    return;
                }
            }

            mTeamHelper.addTeam();
            addListeners();
            TbaDownloader.Companion.load(mTeamHelper.getTeam(), getContext())
                    .addOnSuccessListener(team -> mTeamHelper.updateTeam(team));
        } else {
            Constants.sFirebaseTeams.addChangeEventListener(this);
        }
    }

    private void removeListeners() {
        Constants.sFirebaseTeams.removeChangeEventListener(this);
        if (mPagerAdapter != null) mPagerAdapter.cleanup();
    }

    @Override
    public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
        if (!TextUtils.equals(mTeamHelper.getTeam().getKey(), snapshot.getKey())) return;

        if (type == EventType.REMOVED) {
            onTeamDeleted();
            return;
        } else if (type == EventType.MOVED) return;


        mTeamHelper = Constants.sFirebaseTeams.getObject(index).getHelper();
        mHolder.bind(mTeamHelper);

        if (!mOnScoutingReadyTask.getTask().isComplete()) {
            initScoutList();
            mOnScoutingReadyTask.setResult(null);
        }
    }

    private void initScoutList() {
        TabLayout tabLayout = mRootView.findViewById(R.id.tabs);
        ViewPager viewPager = mRootView.findViewById(R.id.viewpager);
        mPagerAdapter = new ScoutPagerAdapter(this, mHolder, tabLayout, mTeamHelper, getScoutKey());

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

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        if (auth.getCurrentUser() == null) onTeamDeleted();
        else addListeners();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }

    @Override
    public void onDataChanged() { // NOPMD https://github.com/pmd/pmd/issues/347
        // Noop
    }
}
