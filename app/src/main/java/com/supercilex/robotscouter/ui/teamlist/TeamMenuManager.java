package com.supercilex.robotscouter.ui.teamlist;

import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.MenuManager;

import java.util.List;

public interface TeamMenuManager extends MenuManager {
    void onTeamContextMenuRequested(Team team);

    List<Team> getSelectedTeams();

    void onSelectedTeamMoved(Team oldTeam, Team team);

    void onSelectedTeamChanged(Team oldTeam);
}
