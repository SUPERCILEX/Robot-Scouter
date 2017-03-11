package com.supercilex.robotscouter.ui.teamlist;

import com.supercilex.robotscouter.data.util.TeamHelper;

import java.util.List;

public interface TeamMenuManager extends MenuManager {
    void onTeamContextMenuRequested(TeamHelper teamHelper);

    List<TeamHelper> getSelectedTeams();

    void onSelectedTeamMoved(TeamHelper oldTeamHelper, TeamHelper teamHelper);

    void onSelectedTeamRemoved(TeamHelper oldTeamHelper);
}
