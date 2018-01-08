package com.supercilex.robotscouter.data.client

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.data.model.isTrashed
import com.supercilex.robotscouter.util.data.model.ref
import com.supercilex.robotscouter.util.data.teamParser
import com.supercilex.robotscouter.util.logFailures

interface TeamJob {
    val updateTeam: (team: Team, newTeam: Team) -> Unit

    fun startJob(team: Team): Task<Unit> = team.ref.get().logFailures()
            .continueWithTask(AsyncTaskExecutor, Continuation<DocumentSnapshot, Task<Team>> {
                val snapshot = it.result
                if (snapshot.exists()) {
                    val existingTeam = teamParser.parseSnapshot(snapshot)

                    if (existingTeam.isTrashed != false) {
                        return@Continuation Tasks.forResult(null)
                    }

                    startTask(existingTeam)
                } else {
                    snapshot.reference.delete() // Ensure zombies cached on-device die
                    Tasks.forResult(null)
                }
            }).continueWith(AsyncTaskExecutor, Continuation<Team, Unit> {
                updateTeam(team, it.result ?: return@Continuation)
            })

    fun startTask(existingTeam: Team): Task<Team>
}
