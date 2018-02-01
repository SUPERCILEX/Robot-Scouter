package com.supercilex.robotscouter.util.data.model

import android.arch.core.util.Function
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.os.Bundle
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.ListenerRegistrationLifecycleOwner
import com.supercilex.robotscouter.util.data.TeamsLiveData
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.data.toBundle

class TeamHolder : ViewModelBase<Bundle>(),
        Function<ObservableSnapshotArray<Team>, LiveData<Team>> {
    val teamListener: LiveData<Team> = Transformations.switchMap(TeamsLiveData, this)

    private val keepAliveListener = Observer<Team> {}
    private lateinit var team: Team

    override fun onCreate(args: Bundle) {
        team = args.getTeam()
        (teamListener as MutableLiveData).value = team
        teamListener.observe(ListenerRegistrationLifecycleOwner, keepAliveListener)
    }

    override fun apply(teams: ObservableSnapshotArray<Team>?): LiveData<Team> {
        if (teams == null) return MutableLiveData()

        if (team.id.isBlank()) {
            for ((number, id) in teams) {
                if (number == team.number) {
                    team.id = id
                    return apply(teams)
                }
            }

            team.add()
            TbaDownloader.load(team).addOnSuccessListener { team.update(it) }
        }

        return object : MutableLiveData<Team>(), ChangeEventListenerBase {
            override fun onActive() {
                teams.addChangeEventListener(this)
            }

            override fun onInactive() {
                teams.removeChangeEventListener(this)
            }

            override fun onChildChanged(
                    type: ChangeEventType,
                    snapshot: DocumentSnapshot,
                    newIndex: Int,
                    oldIndex: Int
            ) {
                if (teamListener.value?.id != snapshot.id) return

                if (type == ChangeEventType.REMOVED) {
                    value = null
                    return
                } else if (type == ChangeEventType.MOVED) {
                    return
                }

                val newTeam = teams[newIndex]
                if (value != newTeam) value = newTeam.copy()
            }
        }
    }

    fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(teamListener.value?.toBundle() ?: Bundle())

    override fun onCleared() {
        super.onCleared()
        teamListener.removeObserver(keepAliveListener)
    }
}
