package com.supercilex.robotscouter.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.firebase.ui.common.ChangeEventType
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.util.data.PrefObserver
import com.supercilex.robotscouter.util.data.PrefsLiveData
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CompletionHandler
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error

fun initLogging() {
    PrefLogger
}

fun <T> Task<T>.logFailures(ignore: ((Exception) -> Boolean)? = null): Task<T> {
    val trace = generateStackTrace(1)
    return addOnFailureListener {
        if (ignore?.invoke(it)?.not() != false) {
            CrashLogger.onFailure(it.injectRoot(trace))
        }
    }
}

fun <T> Deferred<T>.logFailures(): Deferred<T> {
    invokeOnCompletion(CrashLogger)
    return this
}

fun DocumentReference.log(): DocumentReference {
    logDbUse(path)
    return this
}

fun Query.log(): Query {
    if (this is CollectionReference) return log()

    logDbUse("Unknown")
    return this
}

fun CollectionReference.log(): CollectionReference {
    logDbUse(path)
    return this
}

fun logCrashLog(message: String) {
    Crashlytics.log(message)
    FirebaseCrash.log(message)
    if (BuildConfig.DEBUG) Log.d("CrashLogs", message)
}

fun generateStackTrace(stackTraceSkip: Int) = Thread.currentThread().stackTrace.filter {
    it.className.contains("supercilex")
}.let<List<StackTraceElement>, List<StackTraceElement>> {
    it.subList(1 + stackTraceSkip, it.size)
}

fun Exception.injectRoot(trace: List<StackTraceElement>) = apply {
    stackTrace = stackTrace.toMutableList().apply {
        addAll(0, trace)
        add(trace.size, StackTraceElement("Hack", "startOriginalStackTrace", "Hack.kt", 0))
    }.toTypedArray()
}

@Suppress("NOTHING_TO_INLINE") // Used to minimize pointless stack traces
private inline fun logDbUse(path: String) =
        logCrashLog("Used reference '$path' at ${generateStackTrace(1)}")

object CrashLogger : OnFailureListener, OnCompleteListener<Any>, CompletionHandler, AnkoLogger {
    override fun onFailure(e: Exception) {
        invoke(e)
    }

    override fun onComplete(task: Task<Any>) {
        invoke(task.exception)
    }

    override fun invoke(t: Throwable?) {
        if (t == null || t.javaClass === CancellationException::class.java) return
        if (BuildConfig.DEBUG || isInTestMode) {
            error("An error occurred", t)
        } else {
            Crashlytics.logException(t)
            FirebaseCrash.report(t)
        }
    }
}

private object PrefLogger : PrefObserver() {
    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (type != ChangeEventType.ADDED && type != ChangeEventType.CHANGED) return

        val id = snapshot.id
        val pref = PrefsLiveData.value!![newIndex]
        when (pref) {
            is Boolean -> Crashlytics.setBool(id, pref)
            is String -> Crashlytics.setString(id, pref)
            is Int -> Crashlytics.setInt(id, pref)
            is Long -> Crashlytics.setLong(id, pref)
            is Double -> Crashlytics.setDouble(id, pref)
            is Float -> Crashlytics.setFloat(id, pref)
        }
    }
}
