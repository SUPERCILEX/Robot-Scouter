package com.supercilex.robotscouter.ui.teamlist;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.client.spreadsheet.ExportService;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.TeamSharer;
import com.supercilex.robotscouter.util.data.model.TeamUtilsKt;
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler;

import java.util.ArrayList;
import java.util.List;

import static com.supercilex.robotscouter.util.AuthUtilsKt.isFullUser;
import static com.supercilex.robotscouter.util.ConstantsKt.SINGLE_ITEM;
import static com.supercilex.robotscouter.util.data.IoUtilsKt.getIO_PERMS;
import static com.supercilex.robotscouter.util.ui.FirebaseAdapterUtilsKt.getAdapterItems;
import static com.supercilex.robotscouter.util.ui.FirebaseAdapterUtilsKt.notifyAllItemsChangedNoAnimation;
import static com.supercilex.robotscouter.util.ui.ViewUtilsKt.animateColorChange;

public class TeamMenuHelper implements OnSuccessListener<Void>, ActivityCompat.OnRequestPermissionsResultCallback,
        View.OnClickListener {
    private static final String SELECTED_TEAMS_KEY = "selected_teams_key";

    private final Fragment mFragment;
    private final AppCompatActivity mActivity;
    private final PermissionRequestHandler mPermHandler;

    private final List<Team> mSelectedTeams = new ArrayList<>();

    private final FloatingActionButton mFab;
    private final RecyclerView mRecyclerView;
    private final DrawerLayout mDrawerLayout;
    private final Toolbar mToolbar;
    private FirebaseRecyclerAdapter<Team, TeamViewHolder> mAdapter;

    private boolean mIsMenuReady;

    private MenuItem mSignInItem;

    private MenuItem mExportItem;
    private MenuItem mShareItem;
    private MenuItem mVisitTbaWebsiteItem;
    private MenuItem mVisitTeamWebsiteItem;
    private MenuItem mEditTeamDetailsItem;
    private MenuItem mDeleteItem;

    private Snackbar mSelectAllSnackBar;

    public TeamMenuHelper(Fragment fragment, RecyclerView recyclerView) {
        mFragment = fragment;
        mActivity = ((AppCompatActivity) mFragment.getActivity());
        mRecyclerView = recyclerView;
        mFab = mActivity.findViewById(R.id.fab);
        mDrawerLayout = mActivity.findViewById(R.id.drawer_layout);
        mToolbar = mFragment.getView().findViewById(R.id.toolbar);
        mPermHandler = new PermissionRequestHandler(getIO_PERMS(), mFragment, this);
        initSnackBar();

        mToolbar.setNavigationOnClickListener(this);
    }

    private void initSnackBar() {
        mSelectAllSnackBar = Snackbar.make(
                mFragment.getView(), R.string.multiple_teams_selected, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.select_all, v -> {
                    mSelectedTeams.clear();
                    mSelectedTeams.addAll(getAdapterItems(mAdapter));
                    updateState();
                    notifyItemsChanged();
                });
    }

    public void setAdapter(FirebaseRecyclerAdapter<Team, TeamViewHolder> adapter) {
        mAdapter = adapter;
    }

    public boolean areTeamsSelected() {
        return !mSelectedTeams.isEmpty();
    }

    public void exportAllTeams() {
        mSelectedTeams.addAll(getAdapterItems(mAdapter));
        exportTeams();
    }

    @Override
    public void onClick(View view) {
        if (areTeamsSelected()) resetMenu();
        else mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mIsMenuReady = true;
        inflater.inflate(R.menu.team_options, menu);

        mSignInItem = menu.findItem(R.id.action_sign_in);

        mExportItem = menu.findItem(R.id.action_export_teams);
        mShareItem = menu.findItem(R.id.action_share);
        mVisitTbaWebsiteItem = menu.findItem(R.id.action_visit_tba_website);
        mVisitTeamWebsiteItem = menu.findItem(R.id.action_visit_team_website);
        mEditTeamDetailsItem = menu.findItem(R.id.action_edit_team_details);
        mDeleteItem = menu.findItem(R.id.action_delete);

        updateState();
    }

    private void updateState() {
        if (areTeamsSelected()) {
            setNormalMenuItemsVisible(false);
            setContextMenuItemsVisible(true);
            setTeamSpecificItemsVisible(mSelectedTeams.size() == SINGLE_ITEM);
            updateToolbarTitle();
        }
    }

    public void resetMenu() {
        mSelectedTeams.clear();
        setNormalMenuItemsVisible(true);
        setContextMenuItemsVisible(false);
        setTeamSpecificItemsVisible(false);
        updateToolbarTitle();
        mSelectAllSnackBar.dismiss();
        initSnackBar();
        notifyItemsChanged();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Team team = mSelectedTeams.get(0);
        switch (item.getItemId()) {
            case R.id.action_export_teams:
                exportTeams();
                break;
            case R.id.action_share:
                if (TeamSharer.Companion.shareTeams(mActivity, mSelectedTeams)) {
                    resetMenu();
                }
                break;
            case R.id.action_visit_tba_website:
                TeamUtilsKt.visitTbaWebsite(team, mActivity);
                resetMenu();
                break;
            case R.id.action_visit_team_website:
                TeamUtilsKt.visitTeamWebsite(team, mActivity);
                resetMenu();
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(mFragment.getChildFragmentManager(), team);
                break;
            case R.id.action_delete:
                DeleteTeamDialog.Companion.show(
                        mFragment.getChildFragmentManager(), mSelectedTeams);
                break;
            default:
                return false;
        }
        return true;
    }

    public boolean onBackPressed() {
        if (areTeamsSelected()) {
            resetMenu();
            return true;
        } else {
            return false;
        }
    }

    public void saveState(Bundle outState) {
        outState.putParcelableArray(SELECTED_TEAMS_KEY,
                                    mSelectedTeams.toArray(new Team[mSelectedTeams.size()]));
    }

    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_TEAMS_KEY)) {
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(SELECTED_TEAMS_KEY);
            for (Parcelable parcelable : parcelables) {
                mSelectedTeams.add((Team) parcelable);
            }
            notifyItemsChanged();
        }

        if (mIsMenuReady) updateState();
    }

    public void onTeamContextMenuRequested(Team team) {
        boolean hadNormalMenu = !areTeamsSelected();
        int oldSize = mSelectedTeams.size();

        if (mSelectedTeams.contains(team)) { // Team already selected
            mSelectedTeams.remove(team);
        } else {
            mSelectedTeams.add(team);
        }

        updateToolbarTitle();

        int newSize = mSelectedTeams.size();
        if (hadNormalMenu) {
            updateState();
            notifyItemsChanged();
        } else {
            if (!areTeamsSelected()) { // NOPMD
                resetMenu();
            } else if (newSize == SINGLE_ITEM) {
                setTeamSpecificItemsVisible(true);
            } else {
                setTeamSpecificItemsVisible(false);
                if (newSize > oldSize) mSelectAllSnackBar.show();
            }
        }
    }

    public List<Team> getSelectedTeams() {
        return mSelectedTeams;
    }

    public void onSelectedTeamChanged(Team oldTeam, Team team) {
        mSelectedTeams.remove(oldTeam);
        mSelectedTeams.add(team);
    }

    public void onSelectedTeamRemoved(Team oldTeam) {
        mSelectedTeams.remove(oldTeam);
        if (areTeamsSelected()) {
            updateState();
        } else {
            resetMenu();
        }
    }

    private void setContextMenuItemsVisible(boolean visible) {
        mExportItem.setVisible(visible);
        mShareItem.setVisible(visible);
        mDeleteItem.setVisible(visible);
    }

    private void setTeamSpecificItemsVisible(boolean visible) {
        mVisitTbaWebsiteItem.setVisible(visible);
        mVisitTeamWebsiteItem.setVisible(
                visible && !TextUtils.isEmpty(mSelectedTeams.get(0).getWebsite()));
        mEditTeamDetailsItem.setVisible(visible);

        if (visible) {
            Team team = mSelectedTeams.get(0);

            mVisitTbaWebsiteItem.setTitle(
                    mActivity.getString(R.string.visit_team_website_on_tba, team.getNumber()));
            mVisitTeamWebsiteItem.setTitle(
                    mActivity.getString(R.string.visit_team_website, team.getNumber()));

            mSelectAllSnackBar.dismiss();
        }
    }

    private void setNormalMenuItemsVisible(boolean visible) {
        mSignInItem.setVisible(visible && !isFullUser());

        if (visible) {
            mFab.show();
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED);
            updateToolbarTitle();
        } else {
            mFab.hide();
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        setupIcon(visible);
        updateToolbarColor(visible);
    }

    private void setupIcon(boolean visible) {
        ActionBarDrawerToggle toggle = ((TeamListActivity) mActivity).getDrawerToggle();
        if (visible) {
            toggle.setDrawerIndicatorEnabled(true);
        } else {
            ActionBar actionBar = mActivity.getSupportActionBar();

            actionBar.setDisplayHomeAsUpEnabled(false);
            toggle.setDrawerIndicatorEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void updateToolbarColor(boolean visible) {
        @ColorRes int oldColorPrimary = visible ? R.color.selected_toolbar : R.color.colorPrimary;
        @ColorRes int newColorPrimary = visible ? R.color.colorPrimary : R.color.selected_toolbar;

        if (shouldUpdateBackground(mToolbar.getBackground(), newColorPrimary)) {
            animateColorChange(
                    mActivity,
                    oldColorPrimary,
                    newColorPrimary,
                    animator -> mToolbar.setBackgroundColor((int) animator.getAnimatedValue()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @ColorRes int oldColorPrimaryDark = visible ? R.color.selected_status_bar : R.color.colorPrimaryDark;
            @ColorRes int newColorPrimaryDark = visible ? R.color.colorPrimaryDark : R.color.selected_status_bar;

            if (shouldUpdateBackground(
                    mDrawerLayout.getStatusBarBackgroundDrawable(), newColorPrimaryDark)) {
                animateColorChange(
                        mActivity,
                        oldColorPrimaryDark,
                        newColorPrimaryDark,
                        animator -> mDrawerLayout
                                .setStatusBarBackgroundColor((int) animator.getAnimatedValue()));
            }
        }
    }

    private boolean shouldUpdateBackground(Drawable drawable, int newColor) {
        return !(drawable instanceof ColorDrawable)
                || ((ColorDrawable) drawable).getColor() != ContextCompat.getColor(mActivity,
                                                                                   newColor);
    }

    private void updateToolbarTitle() {
        mActivity.getSupportActionBar().setTitle(
                areTeamsSelected() ?
                        String.valueOf(mSelectedTeams.size()) :
                        mActivity.getString(R.string.app_name));
    }

    private void notifyItemsChanged() {
        notifyAllItemsChangedNoAnimation(mRecyclerView, mAdapter);
    }

    @Override
    public void onSuccess(Void aVoid) {
        exportTeams();
    }

    private void exportTeams() {
        if (ExportService.Companion.exportAndShareSpreadSheet(
                mFragment, mPermHandler, mSelectedTeams)) {
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
