package com.supercilex.robotscouter.util

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

inline fun <T> async(crossinline block: () -> T): Task<T> =
        AsyncTaskExecutor.execute(Callable { block() })

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
