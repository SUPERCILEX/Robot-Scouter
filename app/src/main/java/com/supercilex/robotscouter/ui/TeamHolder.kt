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
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.util.teamsListener

class TeamHolder(app: Application) : ViewModelBase<Bundle>(app),
        Observer<ObservableSnapshotArray<Team>>, ChangeEventListener {
    val teamHelperListener = MutableLiveData<TeamHelper>()

    private val teamHelper: TeamHelper by lazy { teamHelperListener.value!! }
    private lateinit var teams: ObservableSnapshotArray<Team>

    override fun onCreate(args: Bundle) {
        teamHelperListener.value = TeamHelper.parse(args); teamHelper
        teamsListener.observeForever(this)
    }

    override fun onChanged(teams: ObservableSnapshotArray<Team>?) {
        if (teams == null) teamHelperListener.value = null
        else {
            this.teams = teams
            val team = teamHelper.team

            if (TextUtils.isEmpty(team.key)) {
                for (i in teams.indices) {
                    val candidate = teams.getObject(i)
                    if (candidate.numberAsLong == team.numberAsLong) {
                        team.key = candidate.key
                        onChanged(teams)
                        return
                    }
                }

                team.helper.addTeam()
                onChanged(teams)
                TbaDownloader.load(team, getApplication())
                        .addOnSuccessListener { team.helper.updateTeam(it) }
            } else {
                teams.addChangeEventListener(this)
            }
        }
    }

    override fun onChildChanged(type: ChangeEventListener.EventType,
                                snapshot: DataSnapshot,
                                index: Int,
                                oldIndex: Int) {
        if (!TextUtils.equals(teamHelperListener.value!!.team.key, snapshot.key)) return

        if (type == ChangeEventListener.EventType.REMOVED) {
            teamHelperListener.value = null; return
        } else if (type == ChangeEventListener.EventType.MOVED) return

        val newTeam = teams.getObject(index).helper
        if (teamHelperListener.value != newTeam) {
            teamHelperListener.value = Team.Builder(newTeam.team).build().helper
        }
    }

    fun onSaveInstanceState(outState: Bundle) =
            outState.putAll(teamHelperListener.value?.toBundle() ?: Bundle())

    override fun onCleared() {
        super.onCleared()
        teamsListener.removeObserver(this)
        teams.removeChangeEventListener(this)
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())

    override fun onDataChanged() = Unit
}
