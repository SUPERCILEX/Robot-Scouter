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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.gson.Gson
import com.supercilex.robotscouter.common.DeletionType
import com.supercilex.robotscouter.common.FIRESTORE_CONTENT_ID
import com.supercilex.robotscouter.common.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.common.FIRESTORE_SHARE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
import com.supercilex.robotscouter.common.isPolynomial
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.client.startUploadMediaJob
import com.supercilex.robotscouter.core.data.logFailures // ktlint-disable
import com.supercilex.robotscouter.core.data.model.add
import com.supercilex.robotscouter.core.data.model.fetchLatestData
import com.supercilex.robotscouter.core.data.model.forceUpdate
import com.supercilex.robotscouter.core.data.model.isValidTeamUri
import com.supercilex.robotscouter.core.data.model.teamParser
import com.supercilex.robotscouter.core.data.model.teamsQueryGenerator
import com.supercilex.robotscouter.core.data.model.updateTemplateId
import com.supercilex.robotscouter.core.data.model.userPrefsQueryGenerator
import com.supercilex.robotscouter.core.data.model.userRef
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.anko.runOnUiThread
import java.io.File
import java.lang.reflect.Field
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

typealias QueryGenerator = (FirebaseUser) -> Query

val teams = LifecycleAwareFirestoreArray(teamsQueryGenerator, teamParser)
val prefs = LifecycleAwareFirestoreArray(userPrefsQueryGenerator, prefParser)

private val updateLastLogin = object : Runnable {
    override fun run() {
        if (isSignedIn) {
            val lastLogin = mapOf(FIRESTORE_LAST_LOGIN to Date())
            userRef.set(lastLogin, SetOptions.merge()).logFailures(userRef, lastLogin)
        }

        mainHandler.removeCallbacks(this)
        mainHandler.postDelayed(this, TimeUnit.DAYS.toMillis(1))
    }
}

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
        GlobalScope.async(Dispatchers.IO) {
            val media = team.media
            if (!media.isNullOrBlank() && File(media).exists()) {
                team.startUploadMediaJob()
            } else if (media == null || media.isValidTeamUri()) {
                team.fetchLatestData()
                team.indexable.let { FirebaseAppIndex.getInstance().update(it).logFailures(it) }
            } else {
                team.apply {
                    hasCustomMedia = false
                    this.media = null
                    forceUpdate(true)
                }
            }
        }.logFailures()
    }
}

private val dbCacheLock = Mutex()

fun initDatabase() {
    FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
    teams.addChangeEventListener(teamTemplateIdUpdater)
    teams.addChangeEventListener(teamUpdater)

    FirebaseAuth.getInstance().addAuthStateListener {
        val user = it.currentUser
        if (user == null) {
            GlobalScope.async(Dispatchers.IO) {
                dbCacheLock.withLock { dbCache.deleteRecursively() }
            }.logFailures()
        } else {
            updateLastLogin.run()

            User(
                    user.uid,
                    user.email.nullOrFull(),
                    user.phoneNumber.nullOrFull(),
                    user.displayName.nullOrFull(),
                    user.photoUrl?.toString()
            ).smartWrite(userCache) { it.add() }
        }
    }
}

inline fun firestoreBatch(
        transaction: WriteBatch.() -> Unit
) = FirebaseFirestore.getInstance().batch().run {
    transaction()
    commit()
}

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

private inline fun <reified T> T.smartWrite(file: File, crossinline write: (t: T) -> Unit) {
    val new = this
    GlobalScope.async(Dispatchers.IO) {
        val cache = {
            write(new)
            file.safeCreateNewFile().writeText(Gson().toJson(new))
        }

        dbCacheLock.withLock {
            if (file.exists()) {
                val cached = Gson().fromJson(file.readText(), T::class.java)
                if (new != cached) cache()
            } else {
                cache()
            }
        }
    }.logFailures()
}

internal sealed class QueuedDeletion(
        id: String,
        type: DeletionType,
        vararg extras: Pair<String, Any>
) {
    val data = mapOf(id to mapOf(
            FIRESTORE_TYPE to type.id,
            FIRESTORE_TIMESTAMP to Date(),
            *extras
    ))

    class Team(id: String) : QueuedDeletion(id, DeletionType.TEAM)

    class Scout(id: String, teamId: String) :
            QueuedDeletion(id, DeletionType.SCOUT, FIRESTORE_CONTENT_ID to teamId)

    class Template(id: String) : QueuedDeletion(id, DeletionType.TEMPLATE)

    abstract class ShareToken(
            token: String,
            shareType: DeletionType,
            contentIds: List<String>
    ) : QueuedDeletion(
            token,
            DeletionType.SHARE_TOKEN,
            FIRESTORE_SHARE_TYPE to shareType.id,
            FIRESTORE_CONTENT_ID to contentIds
    ) {
        class Team(token: String, teamIds: List<String>) :
                ShareToken(token, DeletionType.TEAM, teamIds)

        class Template(token: String, templateIds: List<String>) :
                ShareToken(token, DeletionType.TEMPLATE, templateIds)
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
                if (dataChangedField.get(array) as Boolean) notifyOnDataChanged()
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
        val dataChangedField: Field = BaseObservableSnapshotArray::class.java
                .getDeclaredField("mHasDataChanged").apply {
                    isAccessible = true
                }

        object KeepAliveListener : ChangeEventListenerBase
    }
}
