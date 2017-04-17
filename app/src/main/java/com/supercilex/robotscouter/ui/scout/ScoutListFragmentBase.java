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
import com.supercilex.robotscouter.data.client.DownloadTeamDataJob;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaDownloader;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.TeamMediaCreator;
import com.supercilex.robotscouter.ui.TeamSender;
import com.supercilex.robotscouter.ui.scout.template.ScoutTemplateSheet;
import com.supercilex.robotscouter.util.AnalyticsHelper;
import com.supercilex.robotscouter.util.ConnectivityHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.Collections;

public abstract class ScoutListFragmentBase extends Fragment
        implements ChangeEventListener, FirebaseAuth.AuthStateListener, TeamMediaCreator.StartCaptureListener {
    public static final String ADD_SCOUT_KEY = "add_scout_key";

    private TeamHelper mTeamHelper;
    protected AppBarViewHolderBase mHolder;
    private ScoutPagerAdapter mPagerAdapter;

    private TaskCompletionSource<Void> mOnScoutingReadyTask = new TaskCompletionSource<>();
    private Bundle mSavedState;

    protected static ScoutListFragmentBase setArgs(TeamHelper teamHelper,
                                                   boolean addScout,
                                                   ScoutListFragmentBase fragment) {
        Bundle args = teamHelper.toBundle();
        args.putBoolean(ADD_SCOUT_KEY, addScout);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTeamHelper = TeamHelper.parse(getArguments());

        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scout_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSavedState = savedInstanceState;

        if (mSavedState == null && ConnectivityHelper.isOffline(getContext())) {
            Snackbar.make(getView().findViewById(R.id.root),
                          R.string.offline_reassurance,
                          Snackbar.LENGTH_LONG)
                    .show();
        }

        mHolder = newAppBarViewHolder(mTeamHelper, mOnScoutingReadyTask.getTask());
        if (savedInstanceState != null) mHolder.restoreState(savedInstanceState);
        mHolder.bind(mTeamHelper);
        addListeners();
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
        Constants.sFirebaseTeams.removeChangeEventListener(this);
        if (mPagerAdapter != null) mPagerAdapter.cleanup();
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPagerAdapter != null) {
            outState.putAll(ScoutUtils.getScoutKeyBundle(mPagerAdapter.getCurrentScoutKey()));
        }
        mHolder.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
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
                mPagerAdapter.setCurrentScoutKey(ScoutUtils.add(mTeamHelper.getTeam()));
                break;
            case R.id.action_add_media:
                ShouldUploadMediaToTbaDialog.show(this);
                break;
            case R.id.action_share:
                TeamSender.launchInvitationIntent(getActivity(),
                                                  Collections.singletonList(mTeamHelper));
                AnalyticsHelper.shareTeam(teamNumber);
                break;
            case R.id.action_visit_tba_team_website:
                mTeamHelper.visitTbaWebsite(getContext());
                break;
            case R.id.action_visit_team_website:
                mTeamHelper.visitTeamWebsite(getContext());
                break;
            case R.id.action_edit_scout_templates:
                DownloadTeamDataJob.cancelAll(getContext());
                ScoutTemplateSheet.show(getChildFragmentManager(), mTeamHelper);
                AnalyticsHelper.editTemplate(teamNumber);
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(getChildFragmentManager(), mTeamHelper);
                AnalyticsHelper.editTeamDetails(teamNumber);
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
            TbaDownloader.load(mTeamHelper.getTeam(), getContext())
                    .addOnSuccessListener(team -> mTeamHelper.updateTeam(team))
                    .addOnFailureListener(getActivity(),
                                          e -> DownloadTeamDataJob.start(getActivity(),
                                                                         mTeamHelper));
        } else {
            Constants.sFirebaseTeams.addChangeEventListener(this);
        }
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
        View view = getView();
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabs);
        ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        String scoutKey = null;

        if (mSavedState != null) scoutKey = ScoutUtils.getScoutKey(mSavedState);
        mPagerAdapter = new ScoutPagerAdapter(this, mHolder, tabLayout, mTeamHelper, scoutKey);

        viewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);


        if (getArguments().getBoolean(ADD_SCOUT_KEY, false)) {
            getArguments().remove(ADD_SCOUT_KEY);
            mPagerAdapter.setCurrentScoutKey(ScoutUtils.add(mTeamHelper.getTeam()));
        }
    }

    protected abstract void onTeamDeleted();

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        if (auth.getCurrentUser() == null) onTeamDeleted();
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
