package com.supercilex.robotscouter.ui.teamlist; // NOPMD

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.BugCatcher;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.AppCompatBase;
import com.supercilex.robotscouter.ui.StickyFragment;
import com.supercilex.robotscouter.ui.common.DeleteTeamDialog;
import com.supercilex.robotscouter.ui.common.TeamDetailsDialog;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TeamListFragment extends StickyFragment
        implements TeamMenuRequestListener, FirebaseAuth.AuthStateListener {
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
        AuthHelper.getAuth().addAuthStateListener(this);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        cleanup();
        if (auth.getCurrentUser() != null) {
            initAdapter();
            if (mRecyclerView != null) mRecyclerView.setAdapter(mAdapter);
        }
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
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                FloatingActionButton fab = getFab();
                if (dy > 0 && fab.getVisibility() == View.VISIBLE) {
                    // User scrolled down and the FAB is currently visible -> hide the FAB
                    fab.hide();
                } else if (dy < 0 && fab.getVisibility() != View.VISIBLE && mSelectedTeams.isEmpty())
                    fab.show();
            }
        });
        return rootView;
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        BaseHelper.restoreRecyclerViewState(savedInstanceState, mAdapter, mManager.get());
        if (savedInstanceState != null && mSelectedTeams.isEmpty()) {
            final Parcelable[] parcelables =
                    savedInstanceState.getParcelableArray(SELECTED_TEAMS_KEY);
            for (Parcelable parcelable : parcelables) {
                mSelectedTeams.add((Team) parcelable);
            }
            notifyItemsChanged();
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
            if (mSelectedTeams.size() == Constants.SINGLE_ITEM) showTeamSpecificItems();
            setToolbarTitle();
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
                resetMenu();
                break;
            case R.id.action_visit_team_website:
                mSelectedTeams.get(0).visitTeamWebsite(getContext());
                resetMenu();
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(mSelectedTeams.get(0), getChildFragmentManager());
                break;
            case R.id.action_delete:
                DeleteTeamDialog.show(getChildFragmentManager(), mSelectedTeams);
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
        AuthHelper.getAuth().removeAuthStateListener(this);
        BugCatcher.getRefWatcher(getActivity()).watch(this);
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
                team.fetchLatestData(getContext());
                teamHolder.bind(team,
                                TeamListFragment.this,
                                TeamListFragment.this,
                                mSelectedTeams.contains(team),
                                !mSelectedTeams.isEmpty());
            }

            @Override
            public void onChanged(EventType type, int index, int oldIndex) {
                switch (type) {
                    case ADDED:
                        break;
                    case CHANGED:
                    case MOVED:
                        for (Team oldTeam : mSelectedTeams) {
                            Team team = getItem(index);
                            if (oldTeam.getKey().equals(team.getKey())) {
                                mSelectedTeams.remove(oldTeam);
                                mSelectedTeams.add(team);
                                break;
                            }
                        }
                        break;
                    case REMOVED:
                        if (!mSelectedTeams.isEmpty()) {
                            List<Team> tmpTeams = new ArrayList<>();
                            for (int i = 0; i < getItemCount(); i++) {
                                tmpTeams.add(getItem(i));
                            }
                            for (Team oldTeam : mSelectedTeams) {
                                if (!tmpTeams.contains(oldTeam)) { // We found the deleted item
                                    mSelectedTeams.remove(oldTeam);
                                    if (mSelectedTeams.isEmpty()) {
                                        resetMenu();
                                    } else {
                                        setToolbarTitle();
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
                super.onChanged(type, index, oldIndex);
            }

            @Override
            public Team getItem(int position) {
                Team team = super.getItem(position);
                team.setKey(getRef(position).getKey());
                return team;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                FirebaseCrash.report(error.toException());
            }
        };
    }

    private void cleanup() {
        if (mAdapter != null) {
            mAdapter.cleanup();
        }
        mRecyclerView.setAdapter(null);
    }

    @Override
    public void onTeamContextMenuRequested(Team team) {
        boolean hadNormalMenu = mSelectedTeams.isEmpty();

        if (mSelectedTeams.contains(team)) { // Team already selected
            mSelectedTeams.remove(team);
        } else {
            mSelectedTeams.add(team);
        }
        setToolbarTitle();

        if (hadNormalMenu) {
            setNormalMenuItemsVisible(false);
            setContextMenuItemsVisible(true);
            showTeamSpecificItems();
            notifyItemsChanged();
        } else {
            if (mSelectedTeams.isEmpty()) {
                resetMenu();
            } else if (mSelectedTeams.size() == Constants.SINGLE_ITEM) {
                showTeamSpecificItems();
            } else {
                hideTeamSpecificMenuItems();
            }
        }
    }

    private void showTeamSpecificItems() {
        Team team = mSelectedTeams.get(0);

        mMenu.findItem(R.id.action_visit_tba_team_website)
                .setVisible(true)
                .setTitle(getString(R.string.visit_team_website_on_tba, team.getNumber()));
        mMenu.findItem(R.id.action_visit_team_website)
                .setVisible(team.getWebsite() != null)
                .setTitle(getString(R.string.visit_team_website, team.getNumber()));
        mMenu.findItem(R.id.action_edit_team_details).setVisible(true);
    }

    private void setContextMenuItemsVisible(boolean visible) {
        mMenu.findItem(R.id.action_share).setVisible(visible);
        mMenu.findItem(R.id.action_delete).setVisible(visible);
        ((AppCompatBase) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(visible);
        if (visible) (getFab()).hide();
    }

    private void setNormalMenuItemsVisible(boolean visible) {
        mMenu.findItem(R.id.action_licenses).setVisible(visible);
        mMenu.findItem(R.id.action_about).setVisible(visible);
        if (visible) {
            (getFab()).show();
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

    private void setToolbarTitle() {
        ((AppCompatBase) getActivity()).getSupportActionBar()
                .setTitle(String.valueOf(mSelectedTeams.size()));
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

    private FloatingActionButton getFab() {
        return (FloatingActionButton) getActivity().findViewById(R.id.fab);
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
