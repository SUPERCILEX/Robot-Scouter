package com.supercilex.robotscouter.core.data.client

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.supercilex.robotscouter.core.logCrashLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal abstract class WorkerBase(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Payload {
        if (runAttemptCount >= MAX_RUN_ATTEMPTS) return Payload(Result.FAILURE)

        return try {
            withContext(Dispatchers.IO) { doBlockingWork() }
        } catch (e: Exception) {
            logCrashLog("$javaClass errored: $e")
            Payload(Result.RETRY)
        }
    }

    protected abstract suspend fun doBlockingWork(): Payload

    private companion object {
        const val MAX_RUN_ATTEMPTS = 7
    }
}
