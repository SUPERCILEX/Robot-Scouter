package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.Observer
import android.os.Bundle
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.UniqueMutableLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.toBundle

class TeamListHolder : ViewModelBase<Bundle?>() {
    val selectedTeamIdListener = UniqueMutableLiveData<Team?>()

    private val keepAliveListener = Observer<ObservableSnapshotArray<Team>> {}

    override fun onCreate(args: Bundle?) {
        selectedTeamIdListener.setValue(args?.getParcelable(TEAM_KEY))
        TeamsLiveData.observeForever(keepAliveListener)
    }

    fun selectTeam(team: Team?) {
        selectedTeamIdListener.setValue(team)
    }

    fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(selectedTeamIdListener.value?.toBundle() ?: Bundle())

    override fun onCleared() {
        super.onCleared()
        TeamsLiveData.removeObserver(keepAliveListener)
    }

    private companion object {
        const val TEAM_KEY = "team_key"
    }
}
