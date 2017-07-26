package com.supercilex.robotscouter.ui

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.text.TextUtils
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader
import com.supercilex.robotscouter.util.addTeam
import com.supercilex.robotscouter.util.parseTeam
import com.supercilex.robotscouter.util.teamsListener
import com.supercilex.robotscouter.util.toBundle
import com.supercilex.robotscouter.util.updateTeam

class TeamHolder(app: Application) : ViewModelBase<Bundle>(app),
        Observer<ObservableSnapshotArray<Team>>, ChangeEventListener {
    val teamListener = MutableLiveData<Team>()

    private val team: Team by lazy { teamListener.value!! }
    private lateinit var teams: ObservableSnapshotArray<Team>

    override fun onCreate(args: Bundle) {
        teamListener.value = parseTeam(args); team
        teamsListener.observeForever(this)
    }

    override fun onChanged(teams: ObservableSnapshotArray<Team>?) {
        if (teams == null) teamListener.value = null
        else {
            this.teams = teams

            if (TextUtils.isEmpty(team.key)) {
                for (i in teams.indices) {
                    val candidate = teams.getObject(i)
                    if (candidate.numberAsLong == team.numberAsLong) {
                        team.key = candidate.key
                        onChanged(teams)
                        return
                    }
                }

                team.addTeam()
                onChanged(teams)
                TbaDownloader.load(team, getApplication())
                        .addOnSuccessListener { team.updateTeam(it) }
            } else {
                teams.addChangeEventListener(this)
            }
        }
    }

    override fun onChildChanged(type: ChangeEventListener.EventType,
                                snapshot: DataSnapshot,
                                index: Int,
                                oldIndex: Int) {
        if (!TextUtils.equals(teamListener.value!!.key, snapshot.key)) return

        if (type == ChangeEventListener.EventType.REMOVED) {
            teamListener.value = null; return
        } else if (type == ChangeEventListener.EventType.MOVED) return

        val newTeam = teams.getObject(index)
        if (teamListener.value != newTeam) {
            teamListener.value = newTeam.copy()
        }
    }

    fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(teamListener.value?.toBundle() ?: Bundle())

    override fun onCleared() {
        super.onCleared()
        teamsListener.removeObserver(this)
        teams.removeChangeEventListener(this)
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())

    override fun onDataChanged() = Unit
}
