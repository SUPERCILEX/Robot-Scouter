package com.supercilex.robotscouter.ui.teamlist;

import com.supercilex.robotscouter.data.model.Team;

public interface TeamSelectionListener {
    void onTeamSelected(Team team, boolean addScout, String scoutKey);

    void saveSelection(Team team);
}
