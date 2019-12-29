package com.supercilex.robotscouter.core.data.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import com.firebase.ui.common.ChangeEventType
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.TEAM_KEY
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TeamHolder(state: SavedStateHandle) : ViewModelBase<Team>(state), ChangeEventListenerBase {
    private val _teamListener = state.getLiveData<Team?>("$TEAM_KEY:holder")
    val teamListener = _teamListener.distinctUntilChanged()

    override fun onCreate(args: Team) {
        if (isSignedIn && args.owners.contains(uid)) {
            if (args.id.isBlank()) {
                GlobalScope.launch(Dispatchers.Main) {
                    for (potentialTeam in teams.waitForChange()) {
                        if (args.number == potentialTeam.number) {
                            _teamListener.value = potentialTeam.copy()
                            return@launch
                        }
                    }

                    args.add()
                    _teamListener.value = args.copy()
                }
            } else {
                _teamListener.value = args
            }
        } else {
            _teamListener.value = null
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
        if (_teamListener.value?.id != snapshot.id) return

        if (type == ChangeEventType.REMOVED) {
            _teamListener.value = null
            return
        } else if (type == ChangeEventType.MOVED) {
            return
        }

        _teamListener.value = teams[newIndex].copy()
    }

    override fun onDataChanged() {
        val current = _teamListener.value ?: return
        if (teams.none { it.id == current.id }) _teamListener.value = null
    }

    override fun onCleared() {
        super.onCleared()
        teams.keepAlive = false
        teams.removeChangeEventListener(this)
    }
}
