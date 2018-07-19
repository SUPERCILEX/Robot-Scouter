package com.supercilex.robotscouter.core.data.client

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.model.isTrashed
import com.supercilex.robotscouter.core.data.model.ref
import com.supercilex.robotscouter.core.data.model.teamParser
import com.supercilex.robotscouter.core.data.parseTeam
import com.supercilex.robotscouter.core.data.toWorkData
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.model.Team

internal abstract class TeamWorker : WorkerBase() {
    abstract val updateTeam: (team: Team, newTeam: Team) -> Unit

    override suspend fun doBlockingWork(): Result {
        val team = inputData.parseTeam()

        // Ensure this job isn't being scheduled after the user has signed out
        if (!team.owners.contains(uid)) return Result.FAILURE

        val snapshot = try {
            team.ref.get().logFailures(team.ref, team).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == Code.PERMISSION_DENIED) {
                return Result.FAILURE // Don't reschedule job
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw e
        }

        if (snapshot.exists()) {
            val existingTeam = teamParser.parseSnapshot(snapshot)

            if (existingTeam.isTrashed != false) return Result.FAILURE

            val newTeam = startTask(existingTeam, team) ?: return Result.FAILURE
            // Recheck since things could have changed since the last check
            if (!team.owners.contains(uid)) return Result.FAILURE

            updateTeam(team, newTeam)
            outputData = newTeam.toWorkData()

            return Result.SUCCESS
        } else {
            snapshot.reference.delete() // Ensure zombies cached on-device die
            return Result.FAILURE
        }
    }

    abstract fun startTask(latestTeam: Team, originalTeam: Team): Team?
}
