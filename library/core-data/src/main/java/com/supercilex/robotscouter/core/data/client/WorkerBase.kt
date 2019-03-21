package com.supercilex.robotscouter.core.data.client

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.supercilex.robotscouter.core.logBreadcrumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal abstract class WorkerBase(
        context: Context,
        workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (runAttemptCount >= MAX_RUN_ATTEMPTS) return Result.failure()

        return try {
            withContext(Dispatchers.IO) { doBlockingWork() }
        } catch (e: Exception) {
            logBreadcrumb("$javaClass errored: $e")
            Result.retry()
        }
    }

    protected abstract suspend fun doBlockingWork(): Result

    private companion object {
        const val MAX_RUN_ATTEMPTS = 7
    }
}
