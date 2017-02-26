package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.AuthHelper;
import com.supercilex.robotscouter.ui.TeamSender;
import com.supercilex.robotscouter.ui.common.TeamDetailsDialog;
import com.supercilex.robotscouter.util.AnalyticsHelper;
import com.supercilex.robotscouter.util.Constants;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class TeamMenuHelper implements TeamMenuManager, EasyPermissions.PermissionCallbacks {
    private static final String SELECTED_TEAMS_KEY = "selected_teams_key";

    /**
     * Do not use.
     * <p>
     * When TeamMenuHelper is initialized, {@link View#findViewById(int)} returns null because
     * setContentView has not yet been called in the Fragment's activity.
     *
     * @see #getFab()
     */
    private FloatingActionButton mFab;

    private Fragment mFragment;
    private RecyclerView mRecyclerView;
    private Menu mMenu;

    private List<TeamHelper> mSelectedTeams = new ArrayList<>();
    private FirebaseRecyclerAdapter mAdapter;

    public TeamMenuHelper(Fragment fragment) {
        mFragment = fragment;
    }

    public void setAdapter(FirebaseRecyclerAdapter adapter) {
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
        mMenu.add(Menu.NONE, R.id.action_export_spreadsheet, Menu.NONE, R.string.export_spreadsheet)
                .setVisible(false)
                .setIcon(R.drawable.ic_import_export_white_24dp)
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
        TeamHelper teamHelper = mSelectedTeams.get(0);
        switch (item.getItemId()) {
            case R.id.action_share:
                if (TeamSender.launchInvitationIntent(mFragment.getActivity(), mSelectedTeams)) {
                    resetMenu();
                }
                AnalyticsHelper.shareTeam(teamHelper.getTeam().getNumber());
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
                AnalyticsHelper.editTeamDetails(teamHelper.getTeam().getNumber());
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
            final Parcelable[] parcelables =
                    savedInstanceState.getParcelableArray(SELECTED_TEAMS_KEY);
            for (Parcelable parcelable : parcelables) {
                mSelectedTeams.add((TeamHelper) parcelable);
            }
            notifyItemsChanged();
        }
    }

    @Override
    public void onTeamContextMenuRequested(TeamHelper teamHelper) {
        boolean hadNormalMenu = mSelectedTeams.isEmpty();

        if (mSelectedTeams.contains(teamHelper)) { // Team already selected
            mSelectedTeams.remove(teamHelper);
        } else {
            mSelectedTeams.add(teamHelper);
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
    public void onSelectedTeamChanged(TeamHelper oldTeamHelper) {
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
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount() + 1);
        ((SimpleItemAnimator) mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(true);
    }

    private FloatingActionButton getFab() {
        if (mFab == null) {
            mFab = (FloatingActionButton) mFragment.getActivity().findViewById(R.id.fab);
        }
        return mFab;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        exportTeams();
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(mFragment, perms)) {
            new AppSettingsDialog.Builder(mFragment).build().show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                && EasyPermissions.hasPermissions(mFragment.getContext(),
                                                  SpreadsheetWriter.PERMS)) {
            exportTeams();
        }
    }

    private void exportTeams() {
        if (SpreadsheetWriter.writeAndShareTeams(mFragment, mSelectedTeams)) resetMenu();
    }
}
