package com.supercilex.robotscouter.ui.teamlist

import android.os.Bundle
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.UniqueMutableLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.teams
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.ui.Saveable

class TeamListHolder : ViewModelBase<Bundle?>(), Saveable {
    val selectedTeamIdListener = UniqueMutableLiveData<Team?>()

    override fun onCreate(args: Bundle?) {
        selectedTeamIdListener.setValue(args?.getParcelable(TEAM_KEY))
        teams.keepAlive = true
    }

    fun selectTeam(team: Team?) {
        selectedTeamIdListener.setValue(team)
    }

    override fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(selectedTeamIdListener.value?.toBundle() ?: Bundle())

    override fun onCleared() {
        super.onCleared()
        teams.keepAlive = false
    }

    private companion object {
        const val TEAM_KEY = "team_key"
    }
}
