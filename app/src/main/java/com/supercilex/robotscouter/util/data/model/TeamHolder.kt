package com.supercilex.robotscouter.util.data.model

import android.app.Application
import android.arch.core.util.Function
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.os.Bundle
import android.text.TextUtils
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.database.DataSnapshot
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.teamsListener

class TeamHolder(app: Application) : ViewModelBase<Bundle>(app),
        Function<ObservableSnapshotArray<Team>, LiveData<Team>> {
    val teamListener: LiveData<Team> = Transformations.switchMap(teamsListener, this)

    private val keepAliveListener = Observer<Team> {}
    private val team: Team by lazy { teamListener.value!! }

    override fun onCreate(args: Bundle) {
        (teamListener as MutableLiveData).value = args.getTeam(); team
        teamListener.observeForever(keepAliveListener)
    }

    override fun apply(teams: ObservableSnapshotArray<Team>?): LiveData<Team> {
        if (teams == null) return MutableLiveData()

        if (TextUtils.isEmpty(team.key)) {
            for (i in teams.indices) {
                val candidate = teams.getObject(i)
                if (candidate.numberAsLong == team.numberAsLong) {
                    team.key = candidate.key
                    return apply(teams)
                }
            }

            team.addTeam()
            TbaDownloader.load(team, getApplication()).addOnSuccessListener { team.updateTeam(it) }
        }

        return object : MutableLiveData<Team>(), ChangeEventListenerBase {
            override fun onActive() {
                teams.addChangeEventListener(this)
            }

            override fun onInactive() {
                teams.removeChangeEventListener(this)
            }

            override fun onChildChanged(type: ChangeEventListener.EventType,
                                        snapshot: DataSnapshot,
                                        index: Int,
                                        oldIndex: Int) {
                if (!TextUtils.equals(teamListener.value?.key, snapshot.key)) return

                if (type == ChangeEventListener.EventType.REMOVED) {
                    value = null; return
                } else if (type == ChangeEventListener.EventType.MOVED) return

                val newTeam = teams.getObject(index)
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
