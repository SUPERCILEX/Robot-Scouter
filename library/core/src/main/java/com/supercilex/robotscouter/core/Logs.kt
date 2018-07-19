package com.supercilex.robotscouter.core

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CompletionHandler
import kotlinx.coroutines.experimental.Deferred

fun <T> Task<T>.logFailures(vararg hints: Any?): Task<T> {
    val trace = generateStackTrace()
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

internal fun generateStackTrace() = Thread.currentThread().stackTrace.let {
    // Skip 2 for the `stackTrace` method, 1 for this method, and 1 for the caller
    it.takeLast(it.size - 4)
}

internal fun Exception.injectRoot(trace: List<StackTraceElement>) = apply {
    stackTrace = stackTrace.toMutableList().apply {
        addAll(0, trace)
        add(trace.size, StackTraceElement("Hack", "startOriginalStackTrace", "Hack.kt", 0))
    }.toTypedArray()
}

object CrashLogger : OnFailureListener, OnCompleteListener<Any>, CompletionHandler {
    override fun onFailure(e: Exception) {
        invoke(e)
    }

    override fun onComplete(task: Task<Any>) {
        invoke(task.exception)
    }

    override fun invoke(t: Throwable?) {
        if (t == null || t.javaClass === CancellationException::class.java) return
        if (BuildConfig.DEBUG || isInTestMode) {
            Log.e("CrashLogger", "An error occurred", t)
        } else {
            Crashlytics.logException(t)
        }
    }
}
