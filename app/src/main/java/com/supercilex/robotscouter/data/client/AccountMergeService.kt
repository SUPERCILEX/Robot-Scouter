package com.supercilex.robotscouter.data.client

import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.util.accountlink.ManualMergeService
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.FIRESTORE_NUMBER
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.model.teamsQuery
import com.supercilex.robotscouter.util.data.updateOwner
import com.supercilex.robotscouter.util.uid
import java.util.Date
import com.supercilex.robotscouter.util.data.model.userPrefs as userPrefsRef

class AccountMergeService : ManualMergeService() {
    private var token: String by LateinitVal()

    private var prevUid: String by LateinitVal()
    private var userPrefs: Task<QuerySnapshot> by LateinitVal()

    private var teams: Task<QuerySnapshot> by LateinitVal()
    private var templates: Task<QuerySnapshot> by LateinitVal()

    override fun onLoadData(): Task<Void?>? {
        token = generateToken

        prevUid = uid!!
        userPrefs = userPrefsRef.get()

        teams = teamsQuery.get()
        templates = getTemplatesQuery().get()

        return Tasks.whenAll(userPrefs, teams, templates).continueWithTask(
                AsyncTaskExecutor, Continuation {
            firestoreBatch {
                val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token)
                (teams.result.toMutableList() + templates.result).map {
                    update(it.reference, tokenPath, Date())
                }
            }
        })
    }

    override fun onTransferData(response: IdpResponse): Task<Void?>? = async {
        for (snapshot in userPrefs.result) {
            userPrefsRef.document(snapshot.id).set(snapshot.data)
        }

        updateOwner(teams.result.map { it.reference }, token, prevUid) { ref ->
            teams.result.find { it.reference.path == ref.path }!!.getLong(FIRESTORE_NUMBER)
        }
        updateOwner(templates.result.map { it.reference }, token, prevUid) { ref ->
            templates.result.find { it.reference.path == ref.path }!!.getDate(FIRESTORE_TIMESTAMP)
        }

        null
    }
}
