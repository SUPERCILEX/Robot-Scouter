package com.supercilex.robotscouter.util.data

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
import com.supercilex.robotscouter.util.FIREBASE_DEFAULT_TEMPLATE
import com.supercilex.robotscouter.util.FIREBASE_METRICS
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.FIREBASE_TEAMS
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.data.model.METRIC_PARSER
import com.supercilex.robotscouter.util.data.model.deleteTeam
import com.supercilex.robotscouter.util.data.model.fetchLatestData
import com.supercilex.robotscouter.util.data.model.getScoutIndicesRef
import com.supercilex.robotscouter.util.data.model.teamIndicesRef
import com.supercilex.robotscouter.util.data.model.templateIndicesRef
import com.supercilex.robotscouter.util.defaultTemplateListener
import com.supercilex.robotscouter.util.isOffline
import java.io.File
import java.util.Arrays
import java.util.Collections

val TEAM_PARSER = SnapshotParser<Team> {
    it.getValue(Team::class.java)!!.apply { key = it.key }
}
val SCOUT_PARSER = SnapshotParser<Scout> {
    Scout(it.child(FIREBASE_NAME).getValue(String::class.java),
            it.child(FIREBASE_METRICS).children.map { METRIC_PARSER.parseSnapshot(it) })
}
private val QUERY_KEY = "query_key"

val ref: DatabaseReference by lazy {
    FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) }.reference
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

fun <T> LiveData<T>.observeOnce(failOnNull: Boolean = true): Task<T> = TaskCompletionSource<T>().apply {
    observeForever(object : Observer<T> {
        override fun onChanged(t: T?) {
            if (t == null && failOnNull) throw NullPointerException() else setResult(t)
            removeObserver(this)
        }
    })
}.task

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
        get() = FirebaseIndexArray(teamIndicesRef.orderByValue(), FIREBASE_TEAMS, TEAM_PARSER)

    private val teamUpdater = object : ChangeEventListenerBase() {
        override fun onChildChanged(type: ChangeEventListener.EventType,
                                    snapshot: DataSnapshot,
                                    index: Int,
                                    oldIndex: Int) {
            if (type == ChangeEventListener.EventType.ADDED || type == ChangeEventListener.EventType.CHANGED) {
                val team = value!!.getObject(index)

                team.fetchLatestData(context)
                val media = team.media
                if (!TextUtils.isEmpty(media) && File(media).exists()) {
                    startUploadTeamMediaJob(context, team)
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
            val rawTeams = ArrayList<Team>()
            for (j in 0 until teams.size) {
                val team = teams.getObject(j)
                val rawTeam = team.copy(key = "", timestamp = 0)

                if (rawTeams.contains(rawTeam)) {
                    mergeTeams(ArrayList(Arrays.asList(
                            teams.getObject(rawTeams.indexOf(rawTeam)),
                            team)))
                    break
                }

                rawTeams.add(rawTeam)
            }
        }

        private fun mergeTeams(teams: MutableList<Team>) {
            Collections.sort(teams)
            val oldTeam = teams.removeAt(0)

            for (team in teams) {
                forceUpdate(getScoutIndicesRef(team.key)).addOnSuccessListener { query ->
                    FirebaseCopier(query, getScoutIndicesRef(oldTeam.key))
                            .performTransformation()
                            .continueWithTask { task -> task.result.ref.removeValue() }
                            .addOnSuccessListener { _ -> team.deleteTeam() }
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
        get() = FirebaseIndexArray(templateIndicesRef, FIREBASE_TEMPLATES, SCOUT_PARSER)
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
