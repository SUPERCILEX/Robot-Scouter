package com.supercilex.robotscouter.util

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

inline fun <T> doAsync(crossinline block: () -> T): Task<T> =
        AsyncTaskExecutor.execute(Callable { block() })

suspend fun <T> Task<T>.await(): T = suspendCoroutine { c: Continuation<T> ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

suspend fun <T> await(vararg jobs: Deferred<T>): List<T> = jobs.map { it.await() }

@Suppress("UNCHECKED_CAST")
fun <T : LifecycleOwner> T.asLifecycleReference(minState: Lifecycle.State = Lifecycle.State.STARTED) =
        LifecycleOwnerRef(asReference(), minState)

object AsyncTaskExecutor : Executor {
    private val service = ThreadPoolExecutor(
            2, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            SynchronousQueue()
    )

    fun <TResult> execute(callable: Callable<TResult>): Task<TResult> = Tasks.call(this, callable)

    override fun execute(runnable: Runnable) {
        service.submit(runnable)
    }
}

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
