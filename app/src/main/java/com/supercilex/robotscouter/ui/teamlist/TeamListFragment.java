package com.supercilex.robotscouter.ui.teamlist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.RobotScouter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.ui.StickyFragment;
import com.supercilex.robotscouter.ui.common.DeleteTeamDialog;
import com.supercilex.robotscouter.ui.common.TeamDetailsDialog;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamListFragment extends StickyFragment implements TeamMenuRequestListener {
    private static final String SELECTED_TEAMS_KEY = "selected_teams_key";

    private RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter mAdapter;
    private WeakReference<LinearLayoutManager> mManager;

    private List<Team> mSelectedTeams = new ArrayList<>();
    private Menu mMenu;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (AuthHelper.isSignedIn()) initAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycler_view, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mManager = new WeakReference<>(new LinearLayoutManager(getContext()));
        mRecyclerView.setLayoutManager(mManager.get());
        mRecyclerView.setAdapter(mAdapter);
        restoreState(savedInstanceState);
        return rootView;
    }

    public void restoreState(@Nullable Bundle savedInstanceState) {
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager.get());
        if (savedInstanceState != null) {
            Team[] teamsArray = (Team[]) savedInstanceState.getParcelableArray(SELECTED_TEAMS_KEY);
            mSelectedTeams = new ArrayList<>(Arrays.asList(teamsArray));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;

        mMenu.add(Menu.NONE, R.id.action_share, Menu.NONE, R.string.share)
                .setVisible(false)
                .setIcon(R.drawable.ic_share_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mMenu.add(Menu.NONE, R.id.action_visit_tba_team_website, Menu.NONE, null)
                .setVisible(false)
                .setIcon(R.drawable.ic_launch_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mMenu.add(Menu.NONE, R.id.action_visit_team_website, Menu.NONE, null)
                .setVisible(false)
                .setIcon(R.drawable.ic_launch_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mMenu.add(Menu.NONE, R.id.action_edit_team_details, Menu.NONE, R.string.edit_team_details)
                .setVisible(false)
                .setIcon(R.drawable.ic_mode_edit_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mMenu.add(Menu.NONE, R.id.action_delete, Menu.NONE, R.string.delete)
                .setVisible(false)
                .setIcon(R.drawable.ic_delete_forever_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if (!mSelectedTeams.isEmpty()) {
            setNormalMenuItemsVisible(false);
            setContextMenuItemsVisible(true);
            if (mSelectedTeams.size() == 1) showTeamSpecificItems(mSelectedTeams.get(0));
            ((AppCompatBase) getActivity()).getSupportActionBar()
                    .setTitle(String.valueOf(mSelectedTeams.size()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                TeamSender.launchInvitationIntent(getActivity(), mSelectedTeams);
                break;
            case R.id.action_visit_tba_team_website:
                mSelectedTeams.get(0).visitTbaWebsite(getContext());
                break;
            case R.id.action_visit_team_website:
                mSelectedTeams.get(0).visitTeamWebsite(getContext());
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(mSelectedTeams.get(0),
                                       getActivity().getSupportFragmentManager());
                break;
            case R.id.action_delete:
                DeleteTeamDialog.show(getActivity().getSupportFragmentManager(), mSelectedTeams);
                break;
            case android.R.id.home:
                resetMenu();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView.setAdapter(null);
        mRecyclerView.setLayoutManager(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        BaseHelper.saveRecyclerViewState(outState, mAdapter, mManager.get());
        outState.putParcelableArray(SELECTED_TEAMS_KEY,
                                    mSelectedTeams.toArray(new Team[mSelectedTeams.size()]));
        super.onSaveInstanceState(outState);
    }

    private void initAdapter() {
        mAdapter = new FirebaseIndexRecyclerAdapter<Team, TeamHolder>(
                Team.class,
                R.layout.team_list_row_layout,
                TeamHolder.class,
                Team.getIndicesRef(),
                Constants.FIREBASE_TEAMS) {
            @Override
            public void populateViewHolder(TeamHolder teamHolder, Team team, int position) {
                team.setKey(getRef(position).getKey());
                team.fetchLatestData(getContext());
                teamHolder.bind(team,
                                TeamListFragment.this,
                                TeamListFragment.this,
                                mSelectedTeams.contains(team),
                                !mSelectedTeams.isEmpty());
            }

            @Override
            public void onChanged(EventType type, int index, int oldIndex) {
                super.onChanged(type, index, oldIndex);
                if (!mSelectedTeams.isEmpty()) resetMenu(); // TODO bad solution
            }

            @Override
            public void onCancelled(DatabaseError error) {
                FirebaseCrash.report(error.toException());
            }
        };
    }

    public void cleanup() {
        mRecyclerView.setAdapter(null);
        if (mAdapter != null) {
            mAdapter.cleanup();
            resetMenu();
        }
    }

    public void resetAdapter() {
        cleanup();
        initAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onTeamContextMenuRequested(Team team) {
        boolean hadNormalMenu = mSelectedTeams.isEmpty();

        if (mSelectedTeams.contains(team)) { // Team already selected
            mSelectedTeams.remove(team);
        } else {
            mSelectedTeams.add(team);
        }
        ((AppCompatBase) getActivity()).getSupportActionBar()
                .setTitle(String.valueOf(mSelectedTeams.size()));

        if (hadNormalMenu) {
            setNormalMenuItemsVisible(false);
            setContextMenuItemsVisible(true);
            showTeamSpecificItems(team);
            notifyItemsChanged();
        } else {
            if (mSelectedTeams.isEmpty()) {
                resetMenu();
            } else if (mSelectedTeams.size() == 1) {
                showTeamSpecificItems(team);
            } else {
                hideTeamSpecificMenuItems();
            }
        }
    }

    private void showTeamSpecificItems(Team team) {
        mMenu.findItem(R.id.action_visit_tba_team_website)
                .setVisible(true)
                .setTitle(getString(R.string.visit_team_website_on_tba, team.getNumber()));
        mMenu.findItem(R.id.action_visit_team_website)
                .setVisible(true)
                .setTitle(getString(R.string.visit_team_website, team.getNumber()));
        mMenu.findItem(R.id.action_edit_team_details).setVisible(true);
    }

    private void setContextMenuItemsVisible(boolean visible) {
        mMenu.findItem(R.id.action_share).setVisible(visible);
        mMenu.findItem(R.id.action_delete).setVisible(visible);
        ((AppCompatBase) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(visible);
        if (visible) ((FloatingActionButton) getActivity().findViewById(R.id.fab)).hide();
    }

    private void setNormalMenuItemsVisible(boolean visible) {
        mMenu.findItem(R.id.action_licenses).setVisible(visible);
        mMenu.findItem(R.id.action_about).setVisible(visible);
        if (visible) {
            ((FloatingActionButton) getActivity().findViewById(R.id.fab)).show();
            ((AppCompatBase) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
            if (AuthHelper.isSignedIn() && !AuthHelper.getUser().isAnonymous()) {
                mMenu.findItem(R.id.action_sign_in).setVisible(false);
                mMenu.findItem(R.id.action_sign_out).setVisible(true);
            } else {
                mMenu.findItem(R.id.action_sign_in).setVisible(true);
                mMenu.findItem(R.id.action_sign_out).setVisible(false);
            }
            hideTeamSpecificMenuItems();
        } else {
            mMenu.findItem(R.id.action_sign_in).setVisible(false);
            mMenu.findItem(R.id.action_sign_out).setVisible(false);
        }
    }

    private void hideTeamSpecificMenuItems() {
        mMenu.findItem(R.id.action_visit_tba_team_website).setVisible(false);
        mMenu.findItem(R.id.action_visit_team_website).setVisible(false);
        mMenu.findItem(R.id.action_edit_team_details).setVisible(false);
    }

    private void resetMenu() {
        setContextMenuItemsVisible(false);
        setNormalMenuItemsVisible(true);
        mSelectedTeams.clear();
        notifyItemsChanged();
    }

    private void notifyItemsChanged() {
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            mAdapter.notifyItemChanged(i);
        }
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(true);
    }

    /**
     * @return true if the back press was consumed, false otherwise.
     */
    public boolean onBackPressed() {
        if (mSelectedTeams.isEmpty()) {
            return false;
        } else {
            resetMenu();
            return true;
        }
    }
}
