package com.supercilex.robotscouter.util.data.model

import android.arch.lifecycle.LiveData
import android.os.Bundle
import com.firebase.ui.common.ChangeEventType
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.UniqueMutableLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.data.teams
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.data.waitForChange
import com.supercilex.robotscouter.util.isSignedIn
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.Saveable
import com.supercilex.robotscouter.util.uid
import kotlinx.coroutines.experimental.async

class TeamHolder : ViewModelBase<Bundle>(), Saveable, ChangeEventListenerBase {
    private val _teamListener = UniqueMutableLiveData<Team?>()
    val teamListener: LiveData<Team?> = _teamListener

    private var called = true

    override fun onCreate(args: Bundle) {
        val team = args.getTeam()
        if (isSignedIn && team.owners.contains(uid)) {
            if (team.id.isBlank()) {
                async {
                    for (potentialTeam in teams.waitForChange()) {
                        if (team.number == potentialTeam.number) {
                            _teamListener.postValue(potentialTeam.copy())
                            return@async
                        }
                    }

                    team.add()
                    _teamListener.postValue(team.copy())

                    try {
                        TbaDownloader.load(team)
                    } catch (e: Exception) {
                        null
                    }?.let { team.update(it) }
                }.logFailures()
            } else {
                _teamListener.setValue(team)
            }
        } else {
            _teamListener.setValue(null)
        }

        teams.keepAlive = true
        teams.addChangeEventListener(this)
    }

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (teamListener.value?.id != snapshot.id) return
        called = true

        if (type == ChangeEventType.REMOVED) {
            _teamListener.setValue(null)
            return
        } else if (type == ChangeEventType.MOVED) {
            return
        }

        _teamListener.setValue(teams[newIndex].copy())
    }

    override fun onDataChanged() {
        if (!called && teams.find { it.id == teamListener.value?.id } == null) {
            _teamListener.setValue(null)
        }

        called = false
    }

    override fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(teamListener.value?.toBundle() ?: Bundle())

    override fun onCleared() {
        super.onCleared()
        teams.keepAlive = false
        teams.removeChangeEventListener(this)
    }
}
