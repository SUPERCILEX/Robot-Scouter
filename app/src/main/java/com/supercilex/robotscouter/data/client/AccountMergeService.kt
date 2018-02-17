package com.supercilex.robotscouter.data.client

import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.util.accountlink.ManualMergeService
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.FIRESTORE_NUMBER
import com.supercilex.robotscouter.util.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.data.model.teamsQuery
import com.supercilex.robotscouter.util.data.updateOwner
import com.supercilex.robotscouter.util.doAsync
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.uid
import java.util.Date
import com.supercilex.robotscouter.util.data.model.userPrefs as userPrefsRef

class AccountMergeService : ManualMergeService() {
    private lateinit var token: String

    private lateinit var prevUid: String
    private lateinit var userPrefs: Task<QuerySnapshot>

    private lateinit var teams: Task<QuerySnapshot>
    private lateinit var templates: Task<QuerySnapshot>

    override fun onLoadData(): Task<Void?>? {
        token = generateToken

        prevUid = uid!!
        userPrefs = userPrefsRef.log().get()

        teams = teamsQuery.log().get()
        templates = getTemplatesQuery().log().get()

        return Tasks.whenAll(userPrefs, teams, templates).continueWithTask(
                AsyncTaskExecutor, Continuation<Void?, Task<Void?>> {
            firestoreBatch {
                val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token)
                (teams.result.toMutableList() + templates.result).map {
                    update(it.reference.log(), tokenPath, Date())
                }
            }
        }).logFailures()
    }

    override fun onTransferData(response: IdpResponse): Task<Void?>? = doAsync {
        for (snapshot in userPrefs.result) {
            userPrefsRef.document(snapshot.id).log().set(snapshot.data).logFailures()
        }

        val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token)

        val teamRefs = teams.result.map { it.reference }
        updateOwner(teamRefs, token, prevUid) { ref ->
            teams.result.find { it.reference.path == ref.path }!!.getLong(FIRESTORE_NUMBER)
        }.continueWithTask {
            Tasks.whenAll(teamRefs.map { it.log().update(tokenPath, FieldValue.delete()) })
        }.logFailures()

        val templateRefs = templates.result.map { it.reference }
        updateOwner(templateRefs, token, prevUid) { ref ->
            templates.result.find { it.reference.path == ref.path }!!.getDate(FIRESTORE_TIMESTAMP)
        }.continueWithTask {
            Tasks.whenAll(templateRefs.map { it.log().update(tokenPath, FieldValue.delete()) })
        }.logFailures()

        null
    }
}
