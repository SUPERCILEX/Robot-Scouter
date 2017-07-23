package com.supercilex.robotscouter.util

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.FirebaseIndexArray
import com.firebase.ui.database.ObservableSnapshotArray
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.supercilex.robotscouter.data.client.startUploadTeamMediaJob
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.FirebaseCopier
import com.supercilex.robotscouter.data.util.METRIC_PARSER
import com.supercilex.robotscouter.data.util.TeamHelper
import com.supercilex.robotscouter.data.util.UserHelper
import com.supercilex.robotscouter.data.util.getScoutIndicesRef
import java.io.File
import java.util.Arrays
import java.util.Collections

val TEAM_PARSER = SnapshotParser<Team> {
    val team: Team = it.getValue(Team::class.java)!!
    team.key = it.key
    team
}
val SCOUT_PARSER = SnapshotParser<Scout> {
    Scout(it.child(FIREBASE_NAME).getValue(String::class.java),
            it.child(FIREBASE_METRICS).children.map { METRIC_PARSER.parseSnapshot(it) })
}
private val QUERY_KEY = "query_key"

val ref: DatabaseReference by lazy {
    val instance = FirebaseDatabase.getInstance()
    instance.setPersistenceEnabled(true)
    instance.reference
}

fun getRefBundle(ref: DatabaseReference) = Bundle().apply {
    putString(QUERY_KEY, ref.toString()
            .split("firebaseio.com/".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1])
}

fun getRef(args: Bundle): DatabaseReference
        = FirebaseDatabase.getInstance().getReference(args.getString(QUERY_KEY))

fun forceUpdate(query: Query): Task<Query> = TaskCompletionSource<Query>().also {
    FirebaseArray(query, Any::class.java).apply {
        addChangeEventListener(object : ChangeEventListener {
            override fun onChildChanged(type: ChangeEventListener.EventType,
                                        snapshot: DataSnapshot,
                                        index: Int,
                                        oldIndex: Int) = setResult()

            override fun onDataChanged() = setResult()

            private fun setResult() {
                removeChangeEventListener(this)
                it.setResult(query)
            }

            override fun onCancelled(error: DatabaseError) = it.setException(error.toException())
        })
    }
}.task

fun <T> LiveData<T>.observeOnce(observer: Observer<T>) = observeForever(object : Observer<T> {
    override fun onChanged(t: T?) {
        observer.onChanged(t)
        removeObserver(this)
    }
})

abstract class ChangeEventListenerBase : ChangeEventListener {
    override fun onChildChanged(type: ChangeEventListener.EventType,
                                snapshot: DataSnapshot,
                                index: Int,
                                oldIndex: Int) = Unit

    override fun onDataChanged() = Unit

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
}

abstract class ObservableSnapshotArrayLiveData<T> : LiveData<ObservableSnapshotArray<T>>(),
        FirebaseAuth.AuthStateListener {
    protected abstract val items: ObservableSnapshotArray<T>

    init {
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        value?.removeAllListeners()
        value = if (auth.currentUser == null) null else items
    }
}

class TeamsLiveData(private val context: Context) : ObservableSnapshotArrayLiveData<Team>() {
    override val items: ObservableSnapshotArray<Team>
        get() = FirebaseIndexArray(TeamHelper.getIndicesRef().orderByValue(), FIREBASE_TEAMS, TEAM_PARSER)

    private val teamUpdater = object : ChangeEventListenerBase() {
        override fun onChildChanged(type: ChangeEventListener.EventType,
                                    snapshot: DataSnapshot,
                                    index: Int,
                                    oldIndex: Int) {
            if (type == ChangeEventListener.EventType.ADDED || type == ChangeEventListener.EventType.CHANGED) {
                val team = value!!.getObject(index)
                val teamHelper = team.helper

                teamHelper.fetchLatestData(context)
                val media = team.media
                if (!TextUtils.isEmpty(media) && File(media).exists()) {
                    startUploadTeamMediaJob(context, teamHelper)
                }
            }
        }
    }
    private val teamMerger = object : ChangeEventListenerBase() {
        override fun onChildChanged(type: ChangeEventListener.EventType,
                                    snapshot: DataSnapshot,
                                    index: Int,
                                    oldIndex: Int) {
            if (isOffline()
                    || !(type == ChangeEventListener.EventType.ADDED || type == ChangeEventListener.EventType.CHANGED)) {
                return
            }

            val teams = value!!
            val rawTeams = ArrayList<TeamHelper>()
            for (j in 0 until teams.size) {
                val team = teams.getObject(j)
                val rawTeam = Team.Builder(team)
                        .setTimestamp(0)
                        .setKey(null)
                        .build()
                        .helper

                if (rawTeams.contains(rawTeam)) {
                    mergeTeams(ArrayList(Arrays.asList(
                            teams.getObject(rawTeams.indexOf(rawTeam)).helper,
                            team.helper)))
                    break
                }

                rawTeams.add(rawTeam)
            }
        }

        private fun mergeTeams(teams: MutableList<TeamHelper>) {
            Collections.sort(teams)
            val oldTeam = teams.removeAt(0).team

            teams.map { it.team }.forEach {
                forceUpdate(getScoutIndicesRef(it.key)).addOnSuccessListener { query ->
                    FirebaseCopier(query, getScoutIndicesRef(oldTeam.key))
                            .performTransformation()
                            .continueWithTask { task -> task.result.ref.removeValue() }
                            .addOnSuccessListener { _ -> it.helper.deleteTeam() }
                }
            }
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        super.onAuthStateChanged(auth)
        if (hasActiveObservers()) onActive()
    }

    override fun onActive() {
        value?.apply {
            if (!isListening(teamUpdater) && !isListening(teamMerger)) {
                addChangeEventListener(teamUpdater)
                addChangeEventListener(teamMerger)
            }
        }
    }

    override fun onInactive() {
        value?.apply {
            removeChangeEventListener(teamUpdater)
            removeChangeEventListener(teamMerger)
        }
    }
}

class TemplatesLiveData : ObservableSnapshotArrayLiveData<Scout>() {
    override val items: ObservableSnapshotArray<Scout>
        get() = FirebaseIndexArray(UserHelper.getScoutTemplateIndicesRef(), FIREBASE_SCOUT_TEMPLATES, SCOUT_PARSER)
}

class DefaultTemplateLiveData : LiveData<DataSnapshot>(), ValueEventListener {
    init {
        FIREBASE_DEFAULT_TEMPLATE.addValueEventListener(this)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        defaultTemplateListener.value = snapshot
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())
}
