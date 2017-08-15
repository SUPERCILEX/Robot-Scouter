package com.supercilex.robotscouter.util.data

import android.arch.core.executor.AppToolkitTaskExecutor
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
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
import com.supercilex.robotscouter.data.model.DEFAULT_TEMPLATE_TYPE
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.FIREBASE_DEFAULT_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_METRICS
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.FIREBASE_PREF_DEFAULT_TEMPLATE_KEY
import com.supercilex.robotscouter.util.FIREBASE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIREBASE_PREF_HAS_SHOWN_EXPORT_HINT
import com.supercilex.robotscouter.util.FIREBASE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.FIREBASE_PREF_NIGHT_MODE
import com.supercilex.robotscouter.util.FIREBASE_PREF_UPLOAD_MEDIA_TO_TBA
import com.supercilex.robotscouter.util.FIREBASE_TEAMS
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.data.model.METRIC_PARSER
import com.supercilex.robotscouter.util.data.model.deleteTeam
import com.supercilex.robotscouter.util.data.model.fetchLatestData
import com.supercilex.robotscouter.util.data.model.getScoutIndicesRef
import com.supercilex.robotscouter.util.data.model.teamIndicesRef
import com.supercilex.robotscouter.util.data.model.templateIndicesRef
import com.supercilex.robotscouter.util.data.model.updateTemplateKey
import com.supercilex.robotscouter.util.data.model.userPrefs
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.templateIndicesListener
import java.io.File
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

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

fun copySnapshots(copySnapshot: DataSnapshot, to: DatabaseReference): Task<Nothing?> =
        HashMap<String, Any?>().let {
            deepCopy(it, copySnapshot)
            to.updateChildren(it).continueWith { null }
        }

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

fun <T> LiveData<T>.observeOnce(isNullable: Boolean = false): Task<T> = TaskCompletionSource<T>().apply {
    val observe = {
        observeForever(object : Observer<T> {
            override fun onChanged(t: T?) {
                setResult(t ?: if (isNullable) null else return)
                task.addOnCompleteListener { removeObserver(this) }
            }
        })
    }

    if (AppToolkitTaskExecutor.getInstance().isMainThread) observe()
    else AppToolkitTaskExecutor.getInstance().postToMainThread { observe() }
}.task

fun <T> LiveData<ObservableSnapshotArray<T>>.observeOnDataChanged(): LiveData<ObservableSnapshotArray<T>> =
        Transformations.switchMap(this) {
            object : MutableLiveData<ObservableSnapshotArray<T>>(), ChangeEventListenerBase {
                override fun onDataChanged() {
                    if (AppToolkitTaskExecutor.getInstance().isMainThread) value = it
                    else postValue(it)
                }

                override fun onActive() {
                    it.addChangeEventListener(this)
                }

                override fun onInactive() {
                    it.removeChangeEventListener(this)
                }
            }
        }

private fun deepCopy(values: MutableMap<String, Any?>, from: DataSnapshot) {
    val children = from.children
    if (children.iterator().hasNext()) {
        for (snapshot in children) {
            val data = HashMap<String, Any?>()
            data.put(".priority", snapshot.priority)
            values.put(snapshot.key, data)

            deepCopy(data, snapshot)
        }
    } else {
        values.put(".value", from.value)
    }
}

interface ChangeEventListenerBase : ChangeEventListener {
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

    val templateKeyUpdater = object : ChangeEventListenerBase {
        override fun onChildChanged(type: ChangeEventListener.EventType,
                                    snapshot: DataSnapshot,
                                    index: Int,
                                    oldIndex: Int) {
            if (type != ChangeEventListener.EventType.ADDED
                    && type != ChangeEventListener.EventType.CHANGED) return

            val team = value!!.getObject(index)
            val templateKey = team.templateKey

            if (templateKey == DEFAULT_TEMPLATE_TYPE) team.updateTemplateKey(defaultTemplateKey)
            else if (templateKey != defaultTemplateKey) {
                templateIndicesListener.observeOnDataChanged().observeOnce().addOnSuccessListener {
                    if (it.map { it.key }.contains(templateKey)) {
                        team.updateTemplateKey(defaultTemplateKey)
                    } else {
                        // If we don't own the template and its data == null, we need to update it
                        // to the default to guarantee data consistency when other devices are
                        // offline or the team is shared with another user. For example, say an
                        // online device deletes a template, but an offline one continues adding
                        // teams with the deleted template. When everything is synced up, we will
                        // end up with teams that have template keys pointing to non-existent
                        // templates. The same thing will happen if another user deletes a template
                        // since that user won’t have access to the other user’s teams to update them.

                        FIREBASE_TEMPLATES.child(templateKey)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.value == null) {
                                            team.updateTemplateKey(defaultTemplateKey)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) =
                                            FirebaseCrash.report(error.toException())
                                })
                    }
                }
            }
        }
    }
    private val updater = object : ChangeEventListenerBase {
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
    private val merger = object : ChangeEventListenerBase {
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

    private val keepAliveListener = Observer<ObservableSnapshotArray<String>> {}

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        super.onAuthStateChanged(auth)
        if (hasActiveObservers()) onActive()
    }

    override fun onActive() {
        value?.apply {
            templateIndicesListener.observeForever(keepAliveListener)

            if (!isListening(templateKeyUpdater)) addChangeEventListener(templateKeyUpdater)
            if (!isListening(updater)) addChangeEventListener(updater)
            if (!isListening(merger)) addChangeEventListener(merger)
        }
    }

    override fun onInactive() {
        value?.apply {
            removeChangeEventListener(templateKeyUpdater)
            removeChangeEventListener(updater)
            removeChangeEventListener(merger)

            templateIndicesListener.removeObserver(keepAliveListener)
        }
    }
}

class TemplateIndicesLiveData : ObservableSnapshotArrayLiveData<String>() {
    override val items: ObservableSnapshotArray<String>
        get() = FirebaseArray(templateIndicesRef, String::class.java)
}

class DefaultTemplatesLiveData : LiveData<ObservableSnapshotArray<Scout>>() {
    init {
        value = FirebaseArray(FIREBASE_DEFAULT_TEMPLATES, SCOUT_PARSER)
                .apply { addChangeEventListener(object : ChangeEventListenerBase {}) }
    }
}

class PrefsLiveData : ObservableSnapshotArrayLiveData<Any>() {
    override val items: ObservableSnapshotArray<Any> get() = FirebaseArray<Any>(userPrefs, PARSER)

    private companion object {
        val PARSER = SnapshotParser<Any> {
            when (it.key) {
                FIREBASE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
                FIREBASE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL,
                FIREBASE_PREF_HAS_SHOWN_EXPORT_HINT
                -> it.getValue(Boolean::class.java)

                FIREBASE_PREF_DEFAULT_TEMPLATE_KEY,
                FIREBASE_PREF_NIGHT_MODE,
                FIREBASE_PREF_UPLOAD_MEDIA_TO_TBA
                -> it.getValue(String::class.java)

                else -> it
            }
        }
    }
}
