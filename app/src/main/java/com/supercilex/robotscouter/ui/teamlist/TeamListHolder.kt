package com.supercilex.robotscouter.ui.teamlist

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.ViewModelBase
import com.supercilex.robotscouter.util.teamsListener

class TeamListHolder(app: Application) : ViewModelBase<Bundle?>(app), Observer<ObservableSnapshotArray<Team>> {
    val selectedTeamKeyListener = MutableLiveData<String?>()

    override fun onCreate(args: Bundle?) {
        selectedTeamKeyListener.value = args?.getString(TEAM_KEY)
        teamsListener.observeForever(this)
    }

    fun onSaveInstanceState(outState: Bundle) = outState.putString(TEAM_KEY, selectedTeamKeyListener.value)

    fun selectTeam(team: Team?) {
        selectedTeamKeyListener.value = team?.key
    }

    override fun onChanged(teams: ObservableSnapshotArray<Team>?) = Unit

    override fun onCleared() = teamsListener.removeObserver(this)

    private companion object {
        const val TEAM_KEY = "team_key"
    }
}
