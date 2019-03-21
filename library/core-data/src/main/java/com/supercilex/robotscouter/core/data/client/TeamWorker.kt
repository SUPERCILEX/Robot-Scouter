package com.supercilex.robotscouter.core.data.client

import android.content.Context
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.model.isTrashed
import com.supercilex.robotscouter.core.data.model.ref
import com.supercilex.robotscouter.core.data.model.teamParser
import com.supercilex.robotscouter.core.data.parseTeam
import com.supercilex.robotscouter.core.data.toWorkData
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.tasks.await

internal abstract class TeamWorker(
        context: Context,
        workerParams: WorkerParameters
) : WorkerBase(context, workerParams) {
    abstract val updateTeam: (team: Team, newTeam: Team) -> Unit

    override suspend fun doBlockingWork(): Result {
        val team = inputData.parseTeam()

        // Ensure this job isn't being scheduled after the user has signed out
        if (!team.owners.contains(uid)) return Result.failure()

        val snapshot = try {
            team.ref.get().logFailures("TeamWorker", team.ref, team).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == Code.PERMISSION_DENIED) {
                return Result.failure() // Don't reschedule job
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw e
        }

        if (snapshot.exists()) {
            val existingTeam = teamParser.parseSnapshot(snapshot)

            if (existingTeam.isTrashed != false) return Result.failure()

            val newTeam = startTask(existingTeam.copy(), team.copy()) ?: return Result.failure()
            // Recheck since things could have changed since the last check
            if (!existingTeam.owners.contains(uid)) return Result.failure()

            updateTeam(existingTeam, newTeam)

            return Result.success(newTeam.toWorkData())
        } else {
            snapshot.reference.delete() // Ensure zombies cached on-device die
            return Result.failure()
        }
    }

    abstract suspend fun startTask(latestTeam: Team, originalTeam: Team): Team?
}
