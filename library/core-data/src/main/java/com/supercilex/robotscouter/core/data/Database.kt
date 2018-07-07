package com.supercilex.robotscouter.core.data

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.common.BaseObservableSnapshotArray
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.ChangeEventListener
import com.firebase.ui.firestore.FirestoreArray
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.common.FIRESTORE_CONTENT_ID
import com.supercilex.robotscouter.common.FIRESTORE_SCOUT_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_SHARE_TOKEN_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_SHARE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TEAM_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.client.startUploadMediaJob
import com.supercilex.robotscouter.core.data.model.fetchLatestData
import com.supercilex.robotscouter.core.data.model.forceUpdateAndRefresh
import com.supercilex.robotscouter.core.data.model.getScoutMetricsRef
import com.supercilex.robotscouter.core.data.model.getScouts
import com.supercilex.robotscouter.core.data.model.getScoutsRef
import com.supercilex.robotscouter.core.data.model.isTrashed
import com.supercilex.robotscouter.core.data.model.isValidTeamUri
import com.supercilex.robotscouter.core.data.model.ref
import com.supercilex.robotscouter.core.data.model.teamParser
import com.supercilex.robotscouter.core.data.model.teamsQueryGenerator
import com.supercilex.robotscouter.core.data.model.trash
import com.supercilex.robotscouter.core.data.model.updateTemplateId
import com.supercilex.robotscouter.core.data.model.userPrefsQueryGenerator
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.jetbrains.anko.runOnUiThread
import java.io.File
import java.lang.reflect.Field
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.suspendCoroutine

typealias QueryGenerator = (FirebaseUser) -> Query

val teams = LifecycleAwareFirestoreArray(teamsQueryGenerator, teamParser)
val prefs = LifecycleAwareFirestoreArray(userPrefsQueryGenerator, prefParser)

private val teamTemplateIdUpdater = object : ChangeEventListenerBase {
    private var nTeamUpdatesForTemplateId = "" to -1

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

        val team = teams[newIndex]
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
private val teamUpdater = object : ChangeEventListenerBase {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

        val team = teams[newIndex]
        async {
            val media = team.media
            if (media?.isNotBlank() == true && File(media).exists()) {
                team.startUploadMediaJob()
            } else if (media == null || media.isValidTeamUri()) {
                team.fetchLatestData()
                team.indexable.let { FirebaseAppIndex.getInstance().update(it).logFailures(it) }
            } else {
                team.apply {
                    hasCustomMedia = false
                    this.media = null
                    forceUpdateAndRefresh()
                }
            }
        }.logFailures()
    }
}
private val teamMerger = object : ChangeEventListenerBase {
    private val mutex = Mutex()

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (isOffline || !(type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED)) {
            return
        }

        val teams = teams.toList()
        async {
            val rawTeams = mutableListOf<Team>()
            for (team in teams) {
                val rawTeam = team.copy(id = "", timestamp = Date(0))

                if (rawTeams.contains(rawTeam)) {
                    val existingTeam = teams[rawTeams.indexOf(rawTeam)]
                    mutex.withLock {
                        if (existingTeam.timestamp < team.timestamp) {
                            mergeTeams(existingTeam, team)
                        } else {
                            mergeTeams(team, existingTeam)
                        }
                    }
                    break
                }

                rawTeams += rawTeam
            }
        }.logFailures()
    }

    private suspend fun mergeTeams(existingTeam: Team, duplicate: Team) {
        try {
            // Blow up if an error occurs retrieving these teams
            val ensureNotTrashed: DocumentSnapshot.() -> Unit = {
                if (checkNotNull(teamParser.parseSnapshot(this).isTrashed)) error("Invalid")
            }
            existingTeam.ref.get().await().ensureNotTrashed()
            duplicate.ref.get().await().ensureNotTrashed()
        } catch (e: Exception) {
            return
        }

        val scouts = duplicate.getScouts()
        firestoreBatch {
            for (scout in scouts) {
                val scoutId = scout.id
                set(existingTeam.getScoutsRef().document(scoutId), scout)
                for (metric in scout.metrics) {
                    set(existingTeam.getScoutMetricsRef(scoutId).document(metric.ref.id), metric)
                }
            }
        }.logFailures(scouts).await()
        duplicate.trash().await()
    }
}

fun initDatabase() {
    FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
    teams.addChangeEventListener(teamTemplateIdUpdater)
    teams.addChangeEventListener(teamUpdater)
    teams.addChangeEventListener(teamMerger)
}

inline fun firestoreBatch(
        transaction: WriteBatch.() -> Unit
) = FirebaseFirestore.getInstance().batch().run {
    transaction()
    commit()
}

inline fun DocumentReference.batch(transaction: WriteBatch.(ref: DocumentReference) -> Unit) =
        firestoreBatch { transaction(this@batch) }

suspend fun Query.getInBatches(batchSize: Long = 100): List<DocumentSnapshot> {
    var query = orderBy(FieldPath.documentId()).limit(batchSize)
    val docs: MutableList<DocumentSnapshot> = query.get().await().documents
    var lastResultSize = docs.size.toLong()

    while (lastResultSize == batchSize) {
        query = query.startAfter(docs.last())
        docs += query.get().await().documents.also {
            lastResultSize = it.size.toLong()
        }
    }

    return docs
}

suspend fun <T> ObservableSnapshotArray<T>.waitForChange(): List<T> = suspendCoroutine {
    // Ensure we're on the same thread that receives db callbacks to prevent CMEs
    RobotScouter.runOnUiThread {
        addChangeEventListener(object : ChangeEventListenerBase {
            override fun onDataChanged() {
                it.resume(toList())
                removeChangeEventListener(this)
            }

            override fun onError(e: FirebaseFirestoreException) {
                it.resumeWithException(e)
                removeChangeEventListener(this)
            }
        })
    }
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
            if (Thread.currentThread().isMain) {
                value = this@asLiveData
            } else {
                postValue(this@asLiveData)
            }
        }
    }
}

sealed class QueuedDeletion(id: String, type: Int, vararg extras: Pair<String, Any>) {
    val data = mapOf(id to mapOf(
            FIRESTORE_TYPE to type,
            FIRESTORE_TIMESTAMP to Date(),
            *extras
    ))

    class Team(id: String) : QueuedDeletion(id, FIRESTORE_TEAM_TYPE)

    class Scout(id: String, teamId: String) :
            QueuedDeletion(id, FIRESTORE_SCOUT_TYPE, FIRESTORE_CONTENT_ID to teamId)

    class Template(id: String) : QueuedDeletion(id, FIRESTORE_TEMPLATE_TYPE)

    abstract class ShareToken(
            token: String,
            shareType: Int,
            contentIds: List<String>
    ) : QueuedDeletion(
            token,
            FIRESTORE_SHARE_TOKEN_TYPE,
            FIRESTORE_SHARE_TYPE to shareType,
            FIRESTORE_CONTENT_ID to contentIds
    ) {
        class Team(token: String, teamIds: List<String>) :
                ShareToken(token, FIRESTORE_TEAM_TYPE, teamIds)

        class Template(token: String, templateIds: List<String>) :
                ShareToken(token, FIRESTORE_TEMPLATE_TYPE, templateIds)
    }
}

class UniqueMutableLiveData<T> : MutableLiveData<T>() {
    private val initialized = AtomicBoolean()

    override fun postValue(value: T) = runIfDifferent(value) { super.postValue(value) }

    override fun setValue(value: T) = runIfDifferent(value) { super.setValue(value) }

    private inline fun runIfDifferent(value: T, block: () -> Unit) {
        if (this.value != value || initialized.compareAndSet(false, true)) block()
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
        logCrashLog("Snapshot listener failure for $javaClass")
        CrashLogger.onFailure(e)
    }
}

class LifecycleAwareFirestoreArray<T>(
        private val generator: QueryGenerator,
        private val parser: SnapshotParser<T>
) : ObservableSnapshotArray<T>(parser) {
    private val _keepAlive = AtomicInteger()
    var keepAlive
        get() = _keepAlive.get() > 0
        set(value) {
            val wasAlive = keepAlive

            if (value) {
                _keepAlive.incrementAndGet()
            } else if (wasAlive) {
                _keepAlive.decrementAndGet()
            }

            if (keepAlive && !wasAlive) {
                lifecycleObserver.onStart(ListenerRegistrationLifecycleOwner)
            } else if (wasAlive && !keepAlive && !isListening) {
                lifecycleObserver.onStop(ListenerRegistrationLifecycleOwner)
            }
        }

    // NEVER use `this` for listeners. Since we're a list, our equals implementation will
    // non-deterministically change, thus breaking common listener addition and removal logic.
    private val authListener = FirebaseAuth.AuthStateListener {
        val user = it.currentUser

        val snapshots = array?.snapshots?.toList() ?: emptyList()
        array?.removeAllListeners()
        array = if (user == null) null else BackingArray(generator(user))

        // Kill old data
        for (i in snapshots.lastIndex downTo 0) {
            notifyOnChildChanged(ChangeEventType.REMOVED, snapshots[i], -1, i)
        }
        if (user == null) notifyOnDataChanged()

        // Reboot array and resume normal operations with new user
        if (isListening) lifecycleObserver.onStart(ListenerRegistrationLifecycleOwner)
    }
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            val array = array ?: return

            if (isCreated.compareAndSet(false, true)) {
                // Make sure listeners are caught up
                for (i in array.indices) {
                    notifyOnChildChanged(ChangeEventType.ADDED, getSnapshot(i), i, -1)
                }
                if (array.isNotEmpty()) notifyOnDataChanged()
            }

            if (!array.isListening(KeepAliveListener)) {
                array.addChangeEventListener(KeepAliveListener)
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            array?.removeChangeEventListener(KeepAliveListener)
            // Destroy stuff since the lifecycle is telling us we can die. We'll reboot later.
            onDestroy()
            removeListenersIfCanDie()
        }
    }
    private val eventForwarder = object : ChangeEventListener {
        override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DocumentSnapshot,
                newIndex: Int,
                oldIndex: Int
        ) = notifyOnChildChanged(type, snapshot, newIndex, oldIndex)

        override fun onDataChanged() = notifyOnDataChanged()

        override fun onError(e: FirebaseFirestoreException) = notifyOnError(e)
    }

    private val overrideListeners = CopyOnWriteArrayList<ChangeEventListener>()

    private val hasAddedListeners = AtomicBoolean()
    private val isCreated = AtomicBoolean()
    private var array: BackingArray? = null

    override val size: Int
        get() {
            val array = array
            return if (array == null || !isCreated.get()) 0 else array.size
        }

    override fun addChangeEventListener(listener: ChangeEventListener): ChangeEventListener {
        if (isListening && array?.isListening == false) {
            // Our lifecycle wants us to be shutdown, but we've received an override request
            overrideListeners += listener
            lifecycleObserver.onStart(ListenerRegistrationLifecycleOwner)
        }
        return super.addChangeEventListener(listener)
    }

    override fun removeChangeEventListener(listener: ChangeEventListener) {
        super.removeChangeEventListener(listener)
        if (overrideListeners.remove(listener) && overrideListeners.isEmpty()) {
            // We're ready to obey the lifecycle commands again
            lifecycleObserver.onStop(ListenerRegistrationLifecycleOwner)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (hasAddedListeners.compareAndSet(false, true)) {
            FirebaseAuth.getInstance().addAuthStateListener(authListener)
            ListenerRegistrationLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        } else {
            // We've been alive this whole time but are getting our first subscriber only now
            lifecycleObserver.onStart(ListenerRegistrationLifecycleOwner)
        }
    }

    override fun onDestroy() {
        isCreated.set(false)
        super.onDestroy()
        if (!keepAlive) array?.removeChangeEventListener(KeepAliveListener)
    }

    override fun get(index: Int) = checkNotNull(array)[index]

    override fun getSnapshot(index: Int) = checkNotNull(array).getSnapshot(index)

    // Don't return `array` here since super will clear us when we're out of listeners, but we don't
    // want that since we're deciding when to kill the backing array.
    override fun getSnapshots(): List<DocumentSnapshot> = mutableListOf()

    private fun removeListenersIfCanDie() {
        if (!keepAlive && hasAddedListeners.compareAndSet(true, false)) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
            ListenerRegistrationLifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    private inner class BackingArray(query: Query) : FirestoreArray<T>(query, parser) {
        @Suppress("UNCHECKED_CAST") // We know its type
        private val listeners = listenersField.get(this) as MutableList<ChangeEventListener>

        init {
            // Ensure events are always forwarded to the real listeners
            listeners += eventForwarder
        }

        // Account for there always being at least 1 listener
        override fun isListening() = listeners.isPolynomial

        public override fun getSnapshots(): List<DocumentSnapshot> = super.getSnapshots()

        override fun onDestroy() {
            super.onDestroy()
            if (!isCreated.get()) removeListenersIfCanDie()
        }
    }

    private companion object {
        val listenersField: Field = BaseObservableSnapshotArray::class.java
                .getDeclaredField("mListeners").apply {
                    isAccessible = true
                }

        object KeepAliveListener : ChangeEventListenerBase
    }
}
