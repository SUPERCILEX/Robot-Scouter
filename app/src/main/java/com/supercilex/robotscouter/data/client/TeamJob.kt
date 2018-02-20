package com.supercilex.robotscouter.data.client

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.data.model.isTrashed
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.data.teamParser
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.uid

interface TeamJob {
    val updateTeam: (team: Team, newTeam: Team) -> Unit

    fun startJob(team: Team): Task<Unit?> {
        // Ensure this job isn't being scheduled after the user has signed out
        if (!team.owners.contains(uid)) return Tasks.forResult(null)

        return team.ref.log().get().continueWith {
            val e = it.exception
            if (e is FirebaseFirestoreException
                && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                null
            } else if (e != null) {
                CrashLogger.invoke(e)
                throw e
            } else {
                it.result
            }
        }.logFailures().continueWithTask(
                AsyncTaskExecutor, Continuation<DocumentSnapshot?, Task<Team?>> {
            val snapshot = it.result ?: return@Continuation Tasks.forResult(null)
            if (snapshot.exists()) {
                val existingTeam = teamParser.parseSnapshot(snapshot)

                if (existingTeam.isTrashed != false) {
                    return@Continuation Tasks.forResult(null)
                }

                startTask(team, existingTeam)
            } else {
                snapshot.reference.delete() // Ensure zombies cached on-device die
                Tasks.forResult(null)
            }
        }).continueWith(AsyncTaskExecutor, Continuation<Team?, Unit> {
            if (team.owners.contains(uid)) updateTeam(team, it.result ?: return@Continuation)
        })
    }

    fun startTask(originalTeam: Team, existingFetchedTeam: Team): Task<Team?>
}
