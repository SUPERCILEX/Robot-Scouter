package com.supercilex.robotscouter.util.data

import android.arch.core.executor.ArchTaskExecutor
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.os.Bundle
import android.text.TextUtils
import com.crashlytics.android.Crashlytics
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ChangeEventListener
import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.firebase.ui.firestore.SnapshotParser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.client.startUploadTeamMediaJob
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.FIRESTORE_METRICS
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_PREF_DEFAULT_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL
import com.supercilex.robotscouter.util.FIRESTORE_PREF_NIGHT_MODE
import com.supercilex.robotscouter.util.FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
import com.supercilex.robotscouter.util.FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.model.fetchLatestData
import com.supercilex.robotscouter.util.data.model.forceUpdate
import com.supercilex.robotscouter.util.data.model.getScoutMetricsRef
import com.supercilex.robotscouter.util.data.model.getScouts
import com.supercilex.robotscouter.util.data.model.getScoutsRef
import com.supercilex.robotscouter.util.data.model.teamsQuery
import com.supercilex.robotscouter.util.data.model.trash
import com.supercilex.robotscouter.util.data.model.updateTemplateId
import com.supercilex.robotscouter.util.data.model.userPrefs
import com.supercilex.robotscouter.util.doAsync
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logFailures
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.runOnUiThread
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.experimental.suspendCoroutine

val teamParser = SnapshotParser {
    it.toObject(Team::class.java).apply { id = it.id }
}
val scoutParser = SnapshotParser { snapshot ->
    Scout(snapshot.id,
          snapshot.getString(FIRESTORE_TEMPLATE_ID),
          snapshot.getString(FIRESTORE_NAME),
          snapshot.getDate(FIRESTORE_TIMESTAMP),
          @Suppress("UNCHECKED_CAST") // Our data is stored as a map of metrics
          (snapshot.data[FIRESTORE_METRICS] as Map<String, Any?>? ?: emptyMap()).map {
              Metric.parse(it.value as Map<String, Any?>, FirebaseFirestore.getInstance().document(
                      "${snapshot.reference.path}/$FIRESTORE_METRICS/${it.key}"))
          })
}
val metricParser = SnapshotParser { Metric.parse(it.data, it.reference) }

private const val REF_KEY = "com.supercilex.robotscouter.REF"

fun initDatabase() {
    FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
    PrefsLiveData
    TeamsLiveData
}

fun Bundle.putRef(ref: DocumentReference) = putString(REF_KEY, ref.path)

fun Bundle.getRef() = FirebaseFirestore.getInstance().document(getString(REF_KEY))

inline fun firestoreBatch(
        transaction: WriteBatch.() -> Unit
) = FirebaseFirestore.getInstance().batch().run {
    transaction()
    commit()
}

inline fun DocumentReference.batch(transaction: WriteBatch.(ref: DocumentReference) -> Unit) =
        firestoreBatch { transaction(this@batch) }.also { log() }

fun Query.getInBatches(batchSize: Long = 100): Task<List<DocumentSnapshot>> = doAsync {
    var query = orderBy(FieldPath.documentId()).limit(batchSize)
    val docs = mutableListOf<DocumentSnapshot>(
            *Tasks.await(query.log().get()).documents.toTypedArray())
    var lastResultSize = docs.size

    while (lastResultSize >= batchSize) {
        query = orderBy(FieldPath.documentId()).startAfter(docs.last()).limit(batchSize)
        docs += Tasks.await(query.log().get()).documents.also {
            lastResultSize = it.size
        }
    }

    docs
}

@Deprecated("Use the coroutine version instead")
inline fun <T, R> LiveData<T>.observeOnce(
        crossinline block: (T) -> Task<R>
): Task<R> = TaskCompletionSource<R>().apply {
    RobotScouter.runOnUiThread {
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

suspend fun <T> LiveData<T>.observeOnce(): T? = suspendCoroutine {
    RobotScouter.runOnUiThread {
        observeForever(object : Observer<T> {
            private val hasChanged = AtomicBoolean()

            override fun onChanged(t: T?) {
                if (hasChanged.compareAndSet(false, true)) it.resume(t)
                removeObserver(this)
            }
        })
    }
}

fun <T> LiveData<ObservableSnapshotArray<T>>.observeOnDataChanged(
): LiveData<ObservableSnapshotArray<T>> = Transformations.switchMap(this) {
    it?.asLiveData()
            // This "cast" is safe because the list is empty and we will never parse anything
            ?: object : ObservableSnapshotArray<T>(@Suppress("UNCHECKED_CAST") { Unit as T }) {
        init {
            notifyOnDataChanged()
        }

        override fun getSnapshots() = emptyList<DocumentSnapshot>().toMutableList()
    }.asLiveData()
}

fun <T> ObservableSnapshotArray<T>.asLiveData(): LiveData<ObservableSnapshotArray<T>> {
    return object : MutableLiveData<ObservableSnapshotArray<T>>(), ChangeEventListenerBase {
        override fun onActive() {
            addChangeEventListener(this)
        }

        override fun onInactive() {
            removeChangeEventListener(this)
        }

        override fun onDataChanged() {
            if (ArchTaskExecutor.getInstance().isMainThread) {
                value = this@asLiveData
            } else {
                postValue(this@asLiveData)
            }
        }
    }
}

class UniqueMutableLiveData<T> : MutableLiveData<T>() {
    override fun postValue(value: T) = runIfDifferent(value) { super.postValue(value) }

    override fun setValue(value: T) = runIfDifferent(value) { super.setValue(value) }

    private inline fun runIfDifferent(value: T, block: () -> Unit) {
        if (this.value != value) block()
    }
}

interface ChangeEventListenerBase : ChangeEventListener {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) = Unit

    override fun onDataChanged() = Unit

    override fun onError(e: FirebaseFirestoreException) {
        FirebaseCrash.report(e)
        Crashlytics.logException(e)
    }
}

object KeepAliveListener : ChangeEventListenerBase

abstract class AuthObservableSnapshotArrayLiveData<T> : LiveData<ObservableSnapshotArray<T>>(),
        FirebaseAuth.AuthStateListener {
    protected abstract val items: ObservableSnapshotArray<T>

    init {
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        value?.removeAllListeners()
        value = if (auth.currentUser == null) {
            null
        } else {
            if (value != null) value = null
            items
        }
    }
}

object TeamsLiveData : AuthObservableSnapshotArrayLiveData<Team>() {
    override val items: ObservableSnapshotArray<Team>
        get() = FirestoreArray(teamsQuery.log(), teamParser)

    private val templateIdUpdater = object : ChangeEventListenerBase {
        private var nTeamUpdatesForTemplateId = "" to -1

        override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DocumentSnapshot,
                newIndex: Int,
                oldIndex: Int
        ) {
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

                team.updateTemplateId(defaultTemplateId)
            }
        }
    }
    private val updater = object : ChangeEventListenerBase {
        override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DocumentSnapshot,
                newIndex: Int,
                oldIndex: Int
        ) {
            if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

            val team = value!![newIndex]
            launch {
                team.fetchLatestData()
                FirebaseAppIndex.getInstance().update(team.indexable).logFailures()

                val media = team.media
                if (!TextUtils.isEmpty(media) && File(media).exists()) {
                    startUploadTeamMediaJob(team)
                }
            }
        }
    }
    private val tokenSanitizer = object : ChangeEventListenerBase {
        override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DocumentSnapshot,
                newIndex: Int,
                oldIndex: Int
        ) {
            if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

            val team = value!![newIndex]

            if (team.activeTokens.isEmpty()) return

            launch {
                val newTeam = team.copy(activeTokens = team.activeTokens.filter {
                    it.value.after(Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_MONTH, -TOKEN_EXPIRATION_DAYS)
                    }.time)
                })

                if (newTeam != team) {
                    newTeam.pendingApprovals = emptyMap()
                    newTeam.forceUpdate()
                }
            }
        }
    }
    private val merger = object : ChangeEventListenerBase {
        override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DocumentSnapshot,
                newIndex: Int,
                oldIndex: Int
        ) {
            if (isOffline() || !(type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED)) {
                return
            }

            val teams = value!!.toList()
            async {
                val rawTeams = mutableListOf<Team>()
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
            }.logFailures()
        }

        private suspend fun mergeTeams(existingTeam: Team, duplicate: Team) {
            val scouts = duplicate.getScouts().await()
            firestoreBatch {
                for (scout in scouts) {
                    val scoutId = scout.id
                    set(existingTeam.getScoutsRef().document(scoutId).log(), scout)
                    for (metric in scout.metrics) {
                        set(existingTeam.getScoutMetricsRef(scoutId).document(metric.ref.id).log(),
                            metric)
                    }
                }
            }.await()
            duplicate.trash()
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
            if (!isListening(tokenSanitizer)) addChangeEventListener(tokenSanitizer)
            if (!isListening(merger)) addChangeEventListener(merger)
        }
    }

    override fun onInactive() {
        value?.apply {
            removeChangeEventListener(templateIdUpdater)
            removeChangeEventListener(updater)
            removeChangeEventListener(tokenSanitizer)
            removeChangeEventListener(merger)
        }
    }
}

object PrefsLiveData : AuthObservableSnapshotArrayLiveData<Any>() {
    override val items: ObservableSnapshotArray<Any>
        get() = FirestoreArray<Any>(userPrefs.log()) {
            val id = it.id
            when (id) {
                FIRESTORE_PREF_HAS_SHOWN_ADD_TEAM_TUTORIAL,
                FIRESTORE_PREF_HAS_SHOWN_SIGN_IN_TUTORIAL,
                FIRESTORE_PREF_SHOULD_SHOW_RATING_DIALOG
                -> it.getBoolean(FIRESTORE_VALUE)

                FIRESTORE_PREF_DEFAULT_TEMPLATE_ID,
                FIRESTORE_PREF_NIGHT_MODE,
                FIRESTORE_PREF_UPLOAD_MEDIA_TO_TBA
                -> it.getString(FIRESTORE_VALUE)

                else -> it
            }
        }
}
