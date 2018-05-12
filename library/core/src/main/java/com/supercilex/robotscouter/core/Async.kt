package com.supercilex.robotscouter.core

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import java.util.concurrent.Future
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun <T> await(vararg jobs: Deferred<T>) = jobs.map { it.await() }

suspend fun <T> List<Deferred<T>>.await() = map { it.await() }

suspend fun <T> Task<T>.await(): T {
    val trace = generateStackTrace(1)
    if (isComplete) return if (isSuccessful) result else throw exception!!.injectRoot(trace)
    return suspendCoroutine { c: Continuation<T> ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resumeWithException(it.injectRoot(trace)) }
    }
}

fun <T> Deferred<T>.asTask(): Task<T> {
    val source = TaskCompletionSource<T>()
    invokeOnCompletion {
        try {
            source.setResult(getCompleted())
        } catch (e: Exception) {
            source.setException(e)
        }
    }
    return source.task
}

fun <T> Future<T>.reportOrCancel(mayInterruptIfRunning: Boolean = false) {
    if (isDone) {
        try {
            get()
        } catch (e: Exception) {
            CrashLogger.onFailure(e)
        }
    }

    cancel(mayInterruptIfRunning)
}

@Suppress("UNCHECKED_CAST")
fun <T : LifecycleOwner> T.asLifecycleReference(minState: Lifecycle.State = Lifecycle.State.STARTED) =
        LifecycleOwnerRef(asReference(), minState)

class LifecycleOwnerRef<out T : LifecycleOwner>(
        private val obj: Ref<T>,
        private val minState: Lifecycle.State
) {
    suspend operator fun invoke(): T {
        val ref = obj()
        if (!ref.lifecycle.currentState.isAtLeast(minState)) throw CancellationException()
        return ref
    }
}
