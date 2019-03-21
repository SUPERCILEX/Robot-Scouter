package com.supercilex.robotscouter.core

import android.util.Log
import androidx.annotation.Keep
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import java.util.concurrent.ExecutionException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

fun logBreadcrumb(message: String, error: Throwable? = null) {
    Crashlytics.log(message)
    if (BuildConfig.DEBUG) Log.d("Breadcrumbs", message)
    CrashLogger.invoke(error)
}

/**
 * Used to bridge a non-coroutine exception into the coroutine world by marking its call site stack
 * trace. Otherwise, we'd only get the stack trace of the remote exception and have no idea where it
 * came from in our code.
 */
class InvocationMarker(actual: Throwable) : ExecutionException(actual)

object CrashLogger : OnFailureListener, OnCompleteListener<Any>, CompletionHandler {
    override fun onFailure(e: Exception) {
        invoke(e)
    }

    override fun onComplete(task: Task<Any>) {
        invoke(task.exception)
    }

    override fun invoke(t: Throwable?) {
        if (t == null || t is CancellationException) return
        if (BuildConfig.DEBUG || isInTestMode) {
            Log.e("CrashLogger", "An error occurred", t)
        } else {
            Crashlytics.logException(t)
        }
    }
}

@Keep
internal class LoggingHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler),
        CoroutineExceptionHandler {
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        CrashLogger.invoke(exception)

        // Since we don't want to crash and Coroutines will call the current thread's handler, we
        // install a noop handler and then reinstall the existing one once coroutines calls the new
        // handler.
        Thread.currentThread().apply {
            // _Do_ crash the main thread to ensure we're not left in a bad state
            if (isMain) return@apply

            val removed = uncaughtExceptionHandler
            uncaughtExceptionHandler = if (removed == null) {
                ResettingHandler
            } else {
                Thread.UncaughtExceptionHandler { t, _ ->
                    t.uncaughtExceptionHandler = removed
                }
            }
        }
    }

    private object ResettingHandler : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            t.uncaughtExceptionHandler = null
        }
    }
}
