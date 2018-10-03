package com.supercilex.robotscouter.core.data.client

import androidx.work.Worker
import com.supercilex.robotscouter.core.logCrashLog
import kotlinx.coroutines.runBlocking

internal abstract class WorkerBase : Worker() {
    override fun doWork() = runBlocking {
        if (runAttemptCount >= MAX_RUN_ATTEMPTS) return@runBlocking Result.FAILURE

        try {
            doBlockingWork()
        } catch (e: Exception) {
            logCrashLog("$javaClass errored: $e")
            Result.RETRY
        }
    }

    protected abstract suspend fun doBlockingWork(): Result

    private companion object {
        const val MAX_RUN_ATTEMPTS = 7
    }
}
