package com.supercilex.robotscouter.util.data

import android.arch.core.executor.ArchTaskExecutor
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.os.Bundle
import android.support.annotation.WorkerThread
import android.text.TextUtils
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ChangeEventListener
import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.firebase.ui.firestore.SnapshotParser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.data.client.startUploadTeamMediaJob
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_NIGHT_MODE
import com.supercilex.robotscouter.util.FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.model.delete
import com.supercilex.robotscouter.util.data.model.fetchLatestData
import com.supercilex.robotscouter.util.data.model.getScoutMetricsRef
import com.supercilex.robotscouter.util.data.model.getScoutRef
import com.supercilex.robotscouter.util.data.model.getScouts
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.model.teamsQuery
import com.supercilex.robotscouter.util.data.model.updateTemplateId
import com.supercilex.robotscouter.util.data.model.userPrefs
import com.supercilex.robotscouter.util.isOffline
import java.io.File
import java.util.Date

val TEAM_PARSER = SnapshotParser<Team> {
    it.toObject(Team::class.java).apply { id = it.id }
}
val SCOUT_PARSER = SnapshotParser<Scout> { snapshot ->
    Scout(snapshot.id,
          snapshot.getString(FIRESTORE_TEMPLATE_ID),
          snapshot.getString(FIRESTORE_NAME),
          snapshot.getDate(FIRESTORE_TIMESTAMP),
          @Suppress("UNCHECKED_CAST")
          (snapshot.data[FIRESTORE_METRICS] as Map<String, Any?>? ?: emptyMap()).map {
              Metric.parse(it.value as Map<String, Any?>, FirebaseFirestore.getInstance().document(
                      "${snapshot.reference.path}/$FIRESTORE_METRICS/${it.key}"))
          })
}
val METRIC_PARSER = SnapshotParser<Metric<*>> { Metric.parse(it.data, it.reference) }

private val REF_KEY = "com.supercilex.robotscouter.REF"

fun initDatabase() {
    PrefsLiveData
    TeamsLiveData
}

fun Bundle.putRef(ref: DocumentReference) = putString(REF_KEY, ref.path)

fun Bundle.getRef() = FirebaseFirestore.getInstance().document(getString(REF_KEY))

inline fun firestoreBatch(transaction: WriteBatch.() -> Unit) = FirebaseFirestore.getInstance().batch().run {
    transaction()
    commit()
}

inline fun DocumentReference.batch(transaction: WriteBatch.(ref: DocumentReference) -> Unit) =
        firestoreBatch { transaction(this@batch) }

/**
 * Delete all documents in a collection. This does **not** automatically discover and delete
 * sub-collections.
 */
fun CollectionReference.delete(batchSize: Long = 100): Task<List<DocumentSnapshot>> = async {
    val deleted = ArrayList<DocumentSnapshot>()

    var query = orderBy(FieldPath.documentId()).limit(batchSize)
    var latestDeleted = deleteQueryBatch(query)
    deleted += latestDeleted

    while (latestDeleted.size >= batchSize) {
        query = orderBy(FieldPath.documentId()).startAfter(latestDeleted.last()).limit(batchSize)
        latestDeleted = deleteQueryBatch(query)
    }

    deleted as List<DocumentSnapshot>
}.addOnFailureListener(CrashLogger)

/** Delete all results from a query in a single [WriteBatch]. */
@WorkerThread
private fun deleteQueryBatch(query: Query): List<DocumentSnapshot> = Tasks.await(query.get()).let {
    Tasks.await(firestoreBatch {
        // TODO remove this when moving to CF delete; breaks when offline
        for (snapshot in it) delete(snapshot.reference)
    })
    it.documents
}

inline fun <T, R> LiveData<T>.observeOnce(crossinline block: (T) -> Task<R>): Task<R> = TaskCompletionSource<R>().apply {
    ArchTaskExecutor.getInstance().executeOnMainThread {
        observeForever(object : Observer<T> {
            private var hasChanged = false

            override fun onChanged(t: T?) {
                if (hasChanged) return
                hasChanged = true

                block(t ?: return).addOnCompleteListener {
                    removeObserver(this)
                    if (it.isSuccessful) setResult(it.result) else setException(it.exception!!)
                }
            }
        })
    }
}.task

fun <T> LiveData<ObservableSnapshotArray<T>>.observeOnDataChanged(): LiveData<ObservableSnapshotArray<T>> =
        Transformations.switchMap(this) {
            object : MutableLiveData<ObservableSnapshotArray<T>>(), ChangeEventListenerBase {
                override fun onDataChanged() {
                    if (ArchTaskExecutor.getInstance().isMainThread) value = it
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

class UniqueMutableLiveData<T> : MutableLiveData<T>() {
    override fun postValue(value: T) {
        runIfDifferent(value) { super.postValue(value) }
    }

    override fun setValue(value: T) {
        runIfDifferent(value) { super.setValue(value) }
    }

    private inline fun runIfDifferent(value: T, block: () -> Unit) {
        if (this.value != value) block()
    }
}

interface ChangeEventListenerBase : ChangeEventListener {
    override fun onChildChanged(type: ChangeEventType,
                                snapshot: DocumentSnapshot,
                                newIndex: Int,
                                oldIndex: Int) = Unit

    override fun onDataChanged() = Unit

    override fun onError(e: FirebaseFirestoreException) = FirebaseCrash.report(e)
}

object KeepAliveListener : ChangeEventListenerBase

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

object TeamsLiveData : ObservableSnapshotArrayLiveData<Team>() {
    override val items: ObservableSnapshotArray<Team>
        get() = FirestoreArray(teamsQuery, TEAM_PARSER)

    private val templateIdUpdater = object : ChangeEventListenerBase {
        private var nTeamUpdatesForTemplateId = "" to -1

        override fun onChildChanged(type: ChangeEventType,
                                    snapshot: DocumentSnapshot,
                                    newIndex: Int,
                                    oldIndex: Int) {
            if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

            val team = value!![newIndex]
            val templateId = team.templateId

            if (templateId != defaultTemplateId) {
                if (team.id == nTeamUpdatesForTemplateId.first) {
                    nTeamUpdatesForTemplateId =
                            nTeamUpdatesForTemplateId.first to nTeamUpdatesForTemplateId.second + 1

                    if (nTeamUpdatesForTemplateId.second >= 5) {
                        // We're probably stuck in a recursive loop where a team is shared between
                        // two devices who each have different default template. Each device is like
                        // "THIS IS THE DEFAULT TEMPLATE! NO, THIS ONE IS YOU SUCKER!" and it
                        // bounces back and forth like that.
                        return
                    }
                } else {
                    nTeamUpdatesForTemplateId = team.id to 0
                }

                TemplateType.coerce(templateId)?.let {
                    team.updateTemplateId(defaultTemplateId)
                    return
                }

                getTemplatesQuery().get().addOnSuccessListener {
                    if (it.documents.map { it.getString(FIRESTORE_TEMPLATE_ID) }
                            .contains(templateId)) {
                        team.updateTemplateId(defaultTemplateId)
                    }
                }.addOnFailureListener(CrashLogger)
            }
        }
    }
    private val updater = object : ChangeEventListenerBase {
        override fun onChildChanged(type: ChangeEventType,
                                    snapshot: DocumentSnapshot,
                                    newIndex: Int,
                                    oldIndex: Int) {
            if (type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED) {
                val team = value!![newIndex]

                team.fetchLatestData()
                val media = team.media
                if (!TextUtils.isEmpty(media) && File(media).exists()) {
                    startUploadTeamMediaJob(team)
                }
            }
        }
    }
    private val merger = object : ChangeEventListenerBase {
        override fun onChildChanged(type: ChangeEventType,
                                    snapshot: DocumentSnapshot,
                                    newIndex: Int,
                                    oldIndex: Int) {
            if (isOffline() || !(type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED)) {
                return
            }

            val teams = ArrayList(value!!)
            async {
                val rawTeams = ArrayList<Team>()
                for (team in teams) {
                    val rawTeam = team.copy(id = "", timestamp = Date(0))

                    if (rawTeams.contains(rawTeam)) {
                        val existingTeam = teams[rawTeams.indexOf(rawTeam)]
                        if (existingTeam.timestamp < team.timestamp) {
                            mergeTeams(existingTeam, team)
                        } else {
                            mergeTeams(team, existingTeam)
                        }
                        break
                    }

                    rawTeams += rawTeam
                }
            }.addOnFailureListener(CrashLogger)
        }

        @WorkerThread
        private fun mergeTeams(existingTeam: Team, duplicate: Team) {
            val scouts = Tasks.await(duplicate.getScouts())
            firestoreBatch {
                for (scout in scouts) {
                    val scoutId = scout.id
                    existingTeam.getScoutRef().document(scoutId).set(scout)
                    for (metric in scout.metrics) {
                        existingTeam.getScoutMetricsRef(scoutId).document(metric.ref.id).set(metric)
                    }
                }
            }.addOnSuccessListener {
                duplicate.delete()
            }
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        super.onAuthStateChanged(auth)
        if (hasActiveObservers()) onActive()
    }

    override fun onActive() {
        value?.apply {
            if (!isListening(templateIdUpdater)) addChangeEventListener(templateIdUpdater)
            if (!isListening(updater)) addChangeEventListener(updater)
            if (!isListening(merger)) addChangeEventListener(merger)
        }
    }

    override fun onInactive() {
        value?.apply {
            removeChangeEventListener(templateIdUpdater)
            removeChangeEventListener(updater)
            removeChangeEventListener(merger)
        }
    }
}

object PrefsLiveData : ObservableSnapshotArrayLiveData<Any>() {
    override val items: ObservableSnapshotArray<Any>
        get() = FirestoreArray<Any>(userPrefs) {
            val id = it.id
            when (id) {
                FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
                FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
                -> it.getBoolean(FIRESTORE_VALUE)

                FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
                FIRESTORE_PREF_NIGHT_MODE,
                FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
                -> it.getString(FIRESTORE_VALUE)

                else -> it
            }
        }
}
