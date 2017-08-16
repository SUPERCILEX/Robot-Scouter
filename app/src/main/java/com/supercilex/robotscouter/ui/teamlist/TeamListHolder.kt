package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase

class TeamListHolder : ViewModelBase<Bundle?>() {
    val selectedTeamKeyListener = MutableLiveData<String?>()

    private val keepAliveListener = Observer<ObservableSnapshotArray<Team>> {}

    override fun onCreate(args: Bundle?) {
        selectedTeamKeyListener.value = args?.getString(TEAM_KEY)
        TeamsLiveData.observeForever(keepAliveListener)
    }

    fun onSaveInstanceState(outState: Bundle) =
            outState.putString(TEAM_KEY, selectedTeamKeyListener.value)

    fun selectTeam(team: Team?) {
        selectedTeamKeyListener.value = team?.key
    }

    override fun onCleared() {
        super.onCleared()
        TeamsLiveData.removeObserver(keepAliveListener)
    }

    private companion object {
        const val TEAM_KEY = "team_key"
    }
}
