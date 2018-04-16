package com.supercilex.robotscouter.feature.teams

import android.os.Bundle
import com.supercilex.robotscouter.core.data.UniqueMutableLiveData
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.Saveable

internal class TeamListHolder : ViewModelBase<Bundle?>(), Saveable {
    val selectedTeamIdListener = UniqueMutableLiveData<Team?>()

    override fun onCreate(args: Bundle?) {
        selectedTeamIdListener.setValue(args?.getParcelable(TEAM_KEY))
        teams.keepAlive = true
    }

    fun selectTeam(team: Team?) {
        selectedTeamIdListener.setValue(team)
    }

    override fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(selectedTeamIdListener.value?.toBundle() ?: Bundle.EMPTY)

    override fun onCleared() {
        super.onCleared()
        teams.keepAlive = false
    }

    private companion object {
        const val TEAM_KEY = "team_key"
    }
}
