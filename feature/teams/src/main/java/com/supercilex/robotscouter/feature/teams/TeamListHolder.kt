package com.supercilex.robotscouter.feature.teams

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.supercilex.robotscouter.core.data.SimpleViewModelBase
import com.supercilex.robotscouter.core.data.TEAM_KEY
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.model.Team

internal class TeamListHolder(state: SavedStateHandle) : SimpleViewModelBase(state) {
    private val _selectedTeamIdListener = state.getLiveData<Team?>(TEAM_KEY)
    val selectedTeamIdListener: LiveData<Team?> get() = _selectedTeamIdListener

    override fun onCreate() {
        teams.keepAlive = true
    }

    fun selectTeam(team: Team?) {
        _selectedTeamIdListener.value = team
    }

    override fun onCleared() {
        super.onCleared()
        teams.keepAlive = false
    }
}
