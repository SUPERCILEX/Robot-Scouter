package com.supercilex.robotscouter.util

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.firebase.ui.common.ChangeEventType
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
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

fun <T> Task<T>.logFailures(
        ref: DocumentReference,
        data: Any? = null
) = logFailures("Path: ${ref.path}", "Data: $data")

fun <T> Task<T>.logFailures(
        refs: List<DocumentReference>,
        data: Any?
) = logFailures("Paths: ${refs.joinToString { it.path }}", "Data: $data")

fun <T> Task<T>.logFailures(vararg hints: Any?): Task<T> {
    val trace = generateStackTrace(2)
    return addOnFailureListener {
        for (hint in hints) logCrashLog(hint.toString())
        CrashLogger.onFailure(it.injectRoot(trace))
    }
}

fun <T> Deferred<T>.logFailures(): Deferred<T> {
    invokeOnCompletion(CrashLogger)
    return this
}

fun logCrashLog(message: String) {
    Crashlytics.log(message)
    if (BuildConfig.DEBUG) Log.d("CrashLogs", message)
}

fun generateStackTrace(stackTraceSkip: Int) = Thread.currentThread().stackTrace.let {
    it.takeLast(it.size - (3 + stackTraceSkip))
}

fun Exception.injectRoot(trace: List<StackTraceElement>) = apply {
    stackTrace = stackTrace.toMutableList().apply {
        addAll(0, trace)
        add(trace.size, StackTraceElement("Hack", "startOriginalStackTrace", "Hack.kt", 0))
    }.toTypedArray()
}

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
