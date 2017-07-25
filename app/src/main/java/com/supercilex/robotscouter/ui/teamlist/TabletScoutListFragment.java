package com.supercilex.robotscouter.ui.teamlist;

import android.arch.lifecycle.LiveData;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.android.gms.tasks.Task;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.scout.AppBarViewHolderBase;
import com.supercilex.robotscouter.ui.scout.ScoutListFragmentBase;

import static com.supercilex.robotscouter.util.ViewUtilsKt.isInTabletMode;

public class TabletScoutListFragment extends ScoutListFragmentBase {
    private View mHint;

    public static ScoutListFragmentBase newInstance(Bundle args) {
        return setArgs(new TabletScoutListFragment(), args);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isInTabletMode(getContext())) {
            TeamSelectionListener listener = (TeamSelectionListener) getContext();
            listener.onTeamSelected(getBundle(), true);
            removeFragment();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHint = getActivity().findViewById(R.id.no_team_selected_hint);
        setHintVisibility(View.GONE);
    }

    @Override
    protected AppBarViewHolderBase newAppBarViewHolder(LiveData<TeamHelper> listener,
                                                       Task<Void> onScoutingReadyTask) {
        return new TabletAppBarViewHolder(listener, onScoutingReadyTask);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setHintVisibility(View.VISIBLE);
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
        public TabletAppBarViewHolder(LiveData<TeamHelper> teamHelper,
                                      Task<Void> onScoutingReadyTask) {
            super(teamHelper, TabletScoutListFragment.this, mRootView, onScoutingReadyTask);
            mToolbar.inflateMenu(R.menu.scout);
            mToolbar.setOnMenuItemClickListener(TabletScoutListFragment.this::onOptionsItemSelected);
            initMenu(mToolbar.getMenu());
        }
    }
}
