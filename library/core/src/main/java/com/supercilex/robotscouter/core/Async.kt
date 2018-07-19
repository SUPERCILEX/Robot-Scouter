package com.supercilex.robotscouter.core

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import java.util.concurrent.Future
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

suspend fun <T> Task<T>.await(): T {
    val trace = generateStackTrace()

    if (isComplete) { // Fast path
        return if (isSuccessful) result else throw checkNotNull(exception).injectRoot(trace)
    }

    return suspendCoroutine { c: Continuation<T> ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resumeWithException(it.injectRoot(trace)) }
    }
}

inline fun <T> Task<T>.fastAddOnSuccessListener(
        activity: Activity? = null,
        crossinline listener: (T) -> Unit
): Task<T> {
    if (isSuccessful) { // Fast path
        listener(result)
        return this
    }

    val crossedListener = OnSuccessListener<T> { listener(it) }
    return if (activity == null) {
        addOnSuccessListener(crossedListener)
    } else {
        addOnSuccessListener(activity, crossedListener)
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

fun <T : LifecycleOwner> T.asLifecycleReference(
        minState: Lifecycle.State = Lifecycle.State.STARTED
) = asLifecycleReference(this, minState)

fun <T : Any> T.asLifecycleReference(
        owner: LifecycleOwner,
        minState: Lifecycle.State = Lifecycle.State.STARTED
) = LifecycleOwnerRef(asReference(), owner.asReference(), minState)

class LifecycleOwnerRef<out T : Any>(
        private val obj: Ref<T>,
        private val owner: Ref<LifecycleOwner>,
        private val minState: Lifecycle.State
) {
    suspend operator fun invoke(): T {
        if (!owner().lifecycle.currentState.isAtLeast(minState)) throw CancellationException()
        return obj()
    }
}
