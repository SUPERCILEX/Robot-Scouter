package com.supercilex.robotscouter.data.client

import com.google.firebase.firestore.FirebaseFirestoreException
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.await
import com.supercilex.robotscouter.util.data.model.isTrashed
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.data.teamParser
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.uid

interface TeamJob {
    val updateTeam: (team: Team, newTeam: Team) -> Unit

    suspend fun startJob(team: Team) {
        // Ensure this job isn't being scheduled after the user has signed out
        if (!team.owners.contains(uid)) return

        val snapshot = try {
            team.ref.log().get().await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return // Don't reschedule job
            } else {
                CrashLogger.onFailure(e)
                throw e
            }
        } catch (e: Exception) {
            CrashLogger.onFailure(e)
            throw e
        }

        if (snapshot.exists()) {
            val existingTeam = teamParser.parseSnapshot(snapshot)

            if (existingTeam.isTrashed != false) return

            val newTeam = startTask(team, existingTeam)
            if (team.owners.contains(uid)) {
                updateTeam(team, newTeam ?: return)
            }
        } else {
            snapshot.reference.delete() // Ensure zombies cached on-device die
        }
    }

    fun startTask(originalTeam: Team, existingFetchedTeam: Team): Team?
}
