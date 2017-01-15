package com.supercilex.robotscouter.ui.teamlist;

import android.support.v4.app.Fragment;

import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseIndexRecyclerAdapter;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;

import java.util.List;

public class TeamListAdapter extends FirebaseIndexRecyclerAdapter<Team, TeamViewHolder> {
    private Fragment mFragment;
    private TeamMenuManager mMenuManager;

    public TeamListAdapter(Fragment fragment, TeamMenuManager menuManager) {
        super(Team.class,
              R.layout.team_list_row_layout,
              TeamViewHolder.class,
              Team.getIndicesRef(),
              Constants.FIREBASE_TEAMS);
        mFragment = fragment;
        mMenuManager = menuManager;
    }

    @Override
    public void populateViewHolder(TeamViewHolder teamHolder, Team team, int position) {
        team.fetchLatestData(mFragment.getContext());
        teamHolder.bind(team,
                        mFragment,
                        mMenuManager,
                        mMenuManager.getSelectedTeams().contains(team),
                        !mMenuManager.getSelectedTeams().isEmpty());
    }

    @Override
    public void onChildChanged(ChangeEventListener.EventType type,
                               int index,
                               int oldIndex) {
        switch (type) {
            case ADDED:
                break;
            case CHANGED:
            case MOVED:
                for (Team oldTeam : mMenuManager.getSelectedTeams()) {
                    Team team = getItem(index);
                    if (oldTeam.getKey().equals(team.getKey())) {
                        mMenuManager.onSelectedTeamMoved(oldTeam, team);
                        break;
                    }
                }
                break;
            case REMOVED:
                if (!mMenuManager.getSelectedTeams().isEmpty()) {
                    List<Team> tmpTeams = getItems();
                    for (Team oldTeam : mMenuManager.getSelectedTeams()) {
                        if (!tmpTeams.contains(oldTeam)) { // We found the deleted item
                            mMenuManager.onSelectedTeamChanged(oldTeam);
                            break;
                        }
                    }
                }
                break;
        }
        super.onChildChanged(type, index, oldIndex);
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
}
