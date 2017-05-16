package com.supercilex.robotscouter.ui.teamlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.scout.AppBarViewHolderBase;
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase;
import com.supercilex.robotscouter.util.ViewUtils;

public class TabletScoutListFragment extends ScoutListFragmentBase {
    private View mHint;

    public static ScoutListFragmentBase newInstance(TeamHelper teamHelper, boolean addScout) {
        return setArgs(teamHelper, addScout, new TabletScoutListFragment());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TeamSelectionListener listener = (TeamSelectionListener) getActivity();
        Team team = TeamHelper.parse(getArguments()).getTeam();
        listener.saveSelection(null);
        listener.saveSelection(team);

        mHint = getActivity().findViewById(R.id.no_team_selected_hint);
        setHintVisibility(View.GONE);
    }

    @Override
    protected AppBarViewHolderBase newAppBarViewHolder(TeamHelper teamHelper,
                                                       Task<Void> onScoutingReadyTask) {
        return new TabletAppBarViewHolder(teamHelper, onScoutingReadyTask);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setHintVisibility(View.VISIBLE);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        if (ViewUtils.isTabletMode(getContext())) {
            super.onAuthStateChanged(auth);
        } else {
            TeamSelectionListener listener = (TeamSelectionListener) getActivity();
            listener.onTeamSelected(
                    TeamHelper.parse(getArguments()).getTeam(),
                    getArguments().getBoolean(ADD_SCOUT_KEY));
            listener.saveSelection(null);
            removeFragment();
        }
    }

    @Override
    protected void onTeamDeleted() {
        removeFragment();
    }

    private void setHintVisibility(int visibility) {
        if (mHint != null) mHint.setVisibility(visibility);
    }

    private void removeFragment() {
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    private class TabletAppBarViewHolder extends AppBarViewHolderBase {
        public TabletAppBarViewHolder(TeamHelper teamHelper, Task<Void> onScoutingReadyTask) {
            super(teamHelper, TabletScoutListFragment.this, mRootView, onScoutingReadyTask);
            mToolbar.inflateMenu(R.menu.scout);
            mToolbar.setOnMenuItemClickListener(TabletScoutListFragment.this::onOptionsItemSelected);
            initMenu(mToolbar.getMenu());
        }
    }
}
