package com.supercilex.robotscouter.ui.teamlist;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetExporter;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.ui.PermissionRequestHandler;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.TeamSharer;
import com.supercilex.robotscouter.util.AnalyticsUtils;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.IoUtils;

import java.util.ArrayList;
import java.util.List;

public class TeamMenuHelper implements TeamMenuManager, OnSuccessListener<Void>, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String SELECTED_TEAMS_KEY = "selected_teams_key";
    private static final int ANIMATION_DURATION = 250;

    private final Fragment mFragment;
    private final PermissionRequestHandler mPermHandler;

    private final List<TeamHelper> mSelectedTeams = new ArrayList<>();

    /**
     * Do not use.
     * <p>
     * When TeamMenuHelper is initialized, {@link View#findViewById(int)} returns null because
     * setContentView has not yet been called in the Fragment's activity.
     *
     * @see #getFab()
     */
    private FloatingActionButton mFab;
    private RecyclerView mRecyclerView;
    private FirebaseRecyclerAdapter<Team, TeamViewHolder> mAdapter;
    private Menu mMenu;

    public TeamMenuHelper(Fragment fragment) {
        mFragment = fragment;
        mPermHandler = new PermissionRequestHandler(IoUtils.PERMS, mFragment, this);
    }

    public void setAdapter(FirebaseRecyclerAdapter<Team, TeamViewHolder> adapter) {
        mAdapter = adapter;
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    public boolean noItemsSelected() {
        return mSelectedTeams.isEmpty();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        mMenu.add(Menu.NONE, R.id.action_export_spreadsheet, Menu.NONE, R.string.export_spreadsheet)
                .setVisible(false)
                .setIcon(R.drawable.ic_import_export_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
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

        updateState();
    }

    private void updateState() {
        if (!mSelectedTeams.isEmpty()) {
            setNormalMenuItemsVisible(false);
            setContextMenuItemsVisible(true);
            int size = mSelectedTeams.size();
            if (size == Constants.SINGLE_ITEM) showTeamSpecificItems();
            setToolbarTitle();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TeamHelper teamHelper = mSelectedTeams.get(0);
        switch (item.getItemId()) {
            case R.id.action_share:
                if (TeamSharer.launchInvitationIntent(mFragment.getActivity(), mSelectedTeams)) {
                    resetMenu();
                }
                AnalyticsUtils.shareTeam(teamHelper.getTeam().getNumber());
                break;
            case R.id.action_visit_tba_team_website:
                teamHelper.visitTbaWebsite(mFragment.getContext());
                resetMenu();
                break;
            case R.id.action_visit_team_website:
                teamHelper.visitTeamWebsite(mFragment.getContext());
                resetMenu();
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(mFragment.getChildFragmentManager(), teamHelper);
                AnalyticsUtils.editTeamDetails(teamHelper.getTeam().getNumber());
                break;
            case R.id.action_export_spreadsheet:
                exportTeams();
                break;
            case R.id.action_delete:
                DeleteTeamDialog.show(mFragment.getChildFragmentManager(), mSelectedTeams);
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
    public boolean onBackPressed() {
        if (mSelectedTeams.isEmpty()) {
            return false;
        } else {
            resetMenu();
            return true;
        }
    }

    @Override
    public void resetMenu() {
        setContextMenuItemsVisible(false);
        setNormalMenuItemsVisible(true);
        mSelectedTeams.clear();
        notifyItemsChanged();
    }

    @Override
    public void saveState(Bundle outState) {
        outState.putParcelableArray(SELECTED_TEAMS_KEY,
                                    mSelectedTeams.toArray(new TeamHelper[mSelectedTeams.size()]));
    }

    @Override
    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null && mSelectedTeams.isEmpty()) {
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(SELECTED_TEAMS_KEY);
            for (Parcelable parcelable : parcelables) {
                mSelectedTeams.add((TeamHelper) parcelable);
            }
            notifyItemsChanged();
        }

        if (mMenu != null) updateState();
    }

    @Override
    public void onTeamContextMenuRequested(TeamHelper teamHelper) {
        boolean hadNormalMenu = mSelectedTeams.isEmpty();

        int oldSize = mSelectedTeams.size();
        if (mSelectedTeams.contains(teamHelper)) { // Team already selected
            mSelectedTeams.remove(teamHelper);
        } else {
            mSelectedTeams.add(teamHelper);
        }
        setToolbarTitle();

        int newSize = mSelectedTeams.size();
        if (hadNormalMenu) {
            setNormalMenuItemsVisible(false);
            setContextMenuItemsVisible(true);
            showTeamSpecificItems();
            notifyItemsChanged();
        } else {
            if (mSelectedTeams.isEmpty()) {
                resetMenu();
            } else if (newSize == Constants.SINGLE_ITEM) {
                showTeamSpecificItems();
            } else {
                hideTeamSpecificMenuItems();
            }

            if (newSize > oldSize && newSize > Constants.SINGLE_ITEM && mAdapter.getItemCount() > newSize) {
                Snackbar.make(mFragment.getView(),
                              R.string.multiple_teams_selected,
                              Snackbar.LENGTH_LONG)
                        .setAction(R.string.select_all, v -> {
                            mSelectedTeams.clear();
                            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                                mSelectedTeams.add(mAdapter.getItem(i).getHelper());
                            }
                            updateState();
                            notifyItemsChanged();
                        })
                        .show();
            }
        }
    }

    @Override
    public List<TeamHelper> getSelectedTeams() {
        return mSelectedTeams;
    }

    @Override
    public void onSelectedTeamMoved(TeamHelper oldTeamHelper, TeamHelper teamHelper) {
        mSelectedTeams.remove(oldTeamHelper);
        mSelectedTeams.add(teamHelper);
    }

    @Override
    public void onSelectedTeamRemoved(TeamHelper oldTeamHelper) {
        mSelectedTeams.remove(oldTeamHelper);
        if (mSelectedTeams.isEmpty()) {
            resetMenu();
        } else {
            setToolbarTitle();
        }
    }

    private void showTeamSpecificItems() {
        Team team = mSelectedTeams.get(0).getTeam();

        mMenu.findItem(R.id.action_visit_tba_team_website)
                .setVisible(true)
                .setTitle(mFragment.getString(R.string.visit_team_website_on_tba,
                                              team.getNumber()));
        mMenu.findItem(R.id.action_visit_team_website)
                .setVisible(team.getWebsite() != null)
                .setTitle(mFragment.getString(R.string.visit_team_website, team.getNumber()));
        mMenu.findItem(R.id.action_edit_team_details).setVisible(true);
    }

    private void setContextMenuItemsVisible(boolean visible) {
        mMenu.findItem(R.id.action_share).setVisible(visible);
        mMenu.findItem(R.id.action_export_spreadsheet).setVisible(visible);
        mMenu.findItem(R.id.action_delete).setVisible(visible);
        ((AppCompatActivity) mFragment.getActivity()).getSupportActionBar()
                .setDisplayHomeAsUpEnabled(visible);
        if (visible) getFab().hide();
    }

    private void setNormalMenuItemsVisible(boolean visible) {
        mMenu.findItem(R.id.action_donate).setVisible(visible);
        mMenu.findItem(R.id.action_licenses).setVisible(visible);
        mMenu.findItem(R.id.action_about).setVisible(visible);

        if (visible) {
            getFab().show();
            ((AppCompatActivity) mFragment.getActivity()).getSupportActionBar()
                    .setTitle(R.string.app_name);

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

        updateToolbarColor(visible);
    }

    private void updateToolbarColor(boolean visible) {
        FragmentActivity activity = mFragment.getActivity();

        @ColorRes int oldColorPrimary = visible ? R.color.selected_toolbar : R.color.colorPrimary;
        @ColorRes int newColorPrimary = visible ? R.color.colorPrimary : R.color.selected_toolbar;

        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
        ValueAnimator toolbarAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(),
                ContextCompat.getColor(mFragment.getContext(), oldColorPrimary),
                ContextCompat.getColor(mFragment.getContext(), newColorPrimary));
        toolbarAnimator.setDuration(ANIMATION_DURATION);
        toolbarAnimator.addUpdateListener(animator -> toolbar.setBackgroundColor((int) animator.getAnimatedValue()));
        toolbarAnimator.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorRes int oldColorPrimaryDark = visible ? R.color.selected_status_bar : R.color.colorPrimaryDark;
            @ColorRes int newColorPrimaryDark = visible ? R.color.colorPrimaryDark : R.color.selected_status_bar;

            ValueAnimator statusBarAnimator = ValueAnimator.ofObject(
                    new ArgbEvaluator(),
                    ContextCompat.getColor(mFragment.getContext(), oldColorPrimaryDark),
                    ContextCompat.getColor(mFragment.getContext(), newColorPrimaryDark));
            statusBarAnimator.setDuration(ANIMATION_DURATION);
            statusBarAnimator.addUpdateListener(animator -> activity.getWindow()
                    .setStatusBarColor((int) animator.getAnimatedValue()));
            statusBarAnimator.start();
        }
    }

    private void hideTeamSpecificMenuItems() {
        mMenu.findItem(R.id.action_visit_tba_team_website).setVisible(false);
        mMenu.findItem(R.id.action_visit_team_website).setVisible(false);
        mMenu.findItem(R.id.action_edit_team_details).setVisible(false);
    }

    private void setToolbarTitle() {
        ((AppCompatActivity) mFragment.getActivity()).getSupportActionBar()
                .setTitle(String.valueOf(mSelectedTeams.size()));
    }

    private void notifyItemsChanged() {
        SimpleItemAnimator animator = (SimpleItemAnimator) mRecyclerView.getItemAnimator();

        animator.setSupportsChangeAnimations(false);
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount() + 1);

        mRecyclerView.post(() -> animator.setSupportsChangeAnimations(true));
    }

    private FloatingActionButton getFab() {
        if (mFab == null) {
            mFab = (FloatingActionButton) mFragment.getActivity().findViewById(R.id.fab);
        }
        return mFab;
    }

    @Override
    public void onSuccess(Void aVoid) {
        exportTeams();
    }

    private void exportTeams() {
        if (SpreadsheetExporter.writeAndShareTeams(mFragment,
                                                   mPermHandler,
                                                   mSelectedTeams)) {
            resetMenu();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mPermHandler.onRequestPermissionsResult(requestCode,
                                                permissions,
                                                grantResults);
    }

    public void onActivityResult(int requestCode) {
        mPermHandler.onActivityResult(requestCode);
    }
}
