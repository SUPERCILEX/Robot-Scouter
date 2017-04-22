package com.supercilex.robotscouter.ui.teamlist;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseAdapterHelper;

import java.util.List;

public class TeamListAdapter extends FirebaseRecyclerAdapter<Team, TeamViewHolder> {
    private final Fragment mFragment;
    private final TeamMenuManager mMenuManager;
    private View mNoTeamsText;

    private String mSelectedTeamKey;

    public TeamListAdapter(Fragment fragment, TeamMenuManager menuManager) {
        super(Constants.sFirebaseTeams, R.layout.team_list_row_layout, TeamViewHolder.class);
        mFragment = fragment;
        mMenuManager = menuManager;

        View view = mFragment.getView();
        if (view != null) mNoTeamsText = view.findViewById(R.id.no_content_hint);
    }

    public void updateSelection(String teamKey) {
        mSelectedTeamKey = teamKey;

        if (TextUtils.isEmpty(mSelectedTeamKey)) {
            View view = mFragment.getView();
            if (view == null) return;

            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
            for (int i = 0; i < getItemCount(); i++) {
                TeamViewHolder viewHolder =
                        (TeamViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder != null && viewHolder.isScouting()) {
                    notifyItemChanged(i);
                    break;
                }
            }
        } else {
            for (int i = 0; i < getItemCount(); i++) {
                if (TextUtils.equals(mSelectedTeamKey, getItem(i).getKey())) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @Override
    public void populateViewHolder(TeamViewHolder teamHolder, Team team, int position) {
        teamHolder.bind(team,
                        mFragment,
                        mMenuManager,
                        mMenuManager.getSelectedTeams().contains(team.getHelper()),
                        !mMenuManager.getSelectedTeams().isEmpty(),
                        TextUtils.equals(mSelectedTeamKey, team.getKey()));

        showNoTeamsHint();
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type,
                               DataSnapshot snapshot,
                               int index,
                               int oldIndex) {
        showNoTeamsHint();

        switch (type) {
            case CHANGED:
            case MOVED:
                for (TeamHelper oldTeam : mMenuManager.getSelectedTeams()) {
                    Team team = getItem(index);
                    if (TextUtils.equals(oldTeam.getTeam().getKey(), team.getKey())) {
                        mMenuManager.onSelectedTeamMoved(oldTeam, team.getHelper());
                        break;
                    }
                }
                break;
            case REMOVED:
                if (!mMenuManager.getSelectedTeams().isEmpty()) {
                    List<Team> tmpTeams = FirebaseAdapterHelper.getItems(this);
                    for (TeamHelper oldTeamHelper : mMenuManager.getSelectedTeams()) {
                        if (!tmpTeams.contains(oldTeamHelper.getTeam())) { // We found the deleted item
                            mMenuManager.onSelectedTeamRemoved(oldTeamHelper);
                            break;
                        }
                    }
                }
                break;
            default:
                // Noop
        }
        super.onChildChanged(type, snapshot, index, oldIndex);
    }

    @Override
    public void onCancelled(DatabaseError error) {
        FirebaseCrash.report(error.toException());
    }

    private void showNoTeamsHint() {
        if (mNoTeamsText != null) {
            mNoTeamsText.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }
}
