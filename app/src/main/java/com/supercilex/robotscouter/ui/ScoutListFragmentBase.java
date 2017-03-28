package com.supercilex.robotscouter.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.ChangeEventListener;
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.supercilex.robotscouter.data.remote.TbaApi;
import com.supercilex.robotscouter.data.util.ScoutUtils;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.scout.AppBarViewHolder;
import com.supercilex.robotscouter.ui.scout.ScoutPagerAdapter;
import com.supercilex.robotscouter.ui.scout.template.ScoutTemplateSheet;
import com.supercilex.robotscouter.util.AnalyticsHelper;
import com.supercilex.robotscouter.util.ConnectivityHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.Collections;

public abstract class ScoutListFragmentBase extends Fragment
        implements ChangeEventListener, OnCompleteListener<Team>, FirebaseAuth.AuthStateListener {
    public static final String ADD_SCOUT_KEY = "add_scout_key";

    private TeamHelper mTeamHelper;
    private AppBarViewHolder mHolder;
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
        mTeamHelper = TeamHelper.get(getArguments());

        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scout_list, container, false);

        if (mSavedState == null && ConnectivityHelper.isOffline(getContext())) {
            Snackbar.make(rootView.findViewById(R.id.root),
                          R.string.offline_reassurance,
                          Snackbar.LENGTH_LONG)
                    .show();
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSavedState = savedInstanceState;
        mHolder = newAppBarViewHolder(mTeamHelper, mOnScoutingReadyTask.getTask());
        mHolder.bind(mTeamHelper);
        addListeners();
    }

    protected abstract AppBarViewHolder newAppBarViewHolder(TeamHelper teamHelper,
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
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.scout, menu);
        mHolder.initMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String teamNumber = mTeamHelper.getTeam().getNumber();
        switch (item.getItemId()) {
            case R.id.action_new_scout:
                mPagerAdapter.setCurrentScoutKey(ScoutUtils.add(mTeamHelper.getTeam()));
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

            mTeamHelper.addTeam(getContext());
            addListeners();
            TbaApi.fetch(mTeamHelper.getTeam(), getContext())
                    .addOnCompleteListener(getActivity(), this);
        } else {
            Constants.sFirebaseTeams.addChangeEventListener(this);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Team> task) {
        if (task.isSuccessful()) {
            mTeamHelper.updateTeam(task.getResult());
        } else {
            DownloadTeamDataJob.start(getContext(), mTeamHelper);
        }
    }

    @Override
    public void onChildChanged(EventType type, DataSnapshot snapshot, int index, int oldIndex) {
        if (type == EventType.REMOVED) {
            if (TextUtils.equals(mTeamHelper.getTeam().getKey(), snapshot.getKey())) {
                onTeamDeleted();
            }
            return;
        } else if (type == EventType.MOVED) return;

        Team team = Constants.sFirebaseTeams.getObject(index);
        if (team.getKey().equals(mTeamHelper.getTeam().getKey())) {
            mTeamHelper = team.getHelper();
            mHolder.bind(mTeamHelper);

            if (!mOnScoutingReadyTask.getTask().isComplete()) {
                TabLayout tabLayout = (TabLayout) getView().findViewById(R.id.tabs);
                ViewPager viewPager = (ViewPager) getView().findViewById(R.id.viewpager);
                String scoutKey = null;

                if (mSavedState != null) scoutKey = ScoutUtils.getScoutKey(mSavedState);
                mPagerAdapter = new ScoutPagerAdapter(this, tabLayout, mTeamHelper, scoutKey);

                viewPager.setAdapter(mPagerAdapter);
                tabLayout.setupWithViewPager(viewPager);


                if (getArguments().getBoolean(ADD_SCOUT_KEY, false)) {
                    getArguments().remove(ADD_SCOUT_KEY);
                    mPagerAdapter.setCurrentScoutKey(ScoutUtils.add(mTeamHelper.getTeam()));
                }

                mOnScoutingReadyTask.setResult(null);
            }
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
    public void onDataChanged() { // NOPMD
        // Noop
    }
}
