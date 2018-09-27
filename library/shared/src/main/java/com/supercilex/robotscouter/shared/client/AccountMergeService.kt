package com.supercilex.robotscouter.shared.client

import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.util.accountlink.ManualMergeService
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.core.await
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.model.getTemplatesQuery
import com.supercilex.robotscouter.core.data.model.shareTeams
import com.supercilex.robotscouter.core.data.model.shareTemplates
import com.supercilex.robotscouter.core.data.model.teamsQuery
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.updateOwner
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.tasks.asTask
import com.supercilex.robotscouter.core.data.model.userPrefs as userPrefsRef

internal class AccountMergeService : ManualMergeService() {
    private lateinit var instance: Instance

    override fun onLoadData(): Task<Void?>? {
        val prevUid = checkNotNull(uid)
        logCrashLog("Migrating user data from $prevUid.")

        return GlobalScope.async {
            lateinit var userPrefs: QuerySnapshot
            lateinit var teams: QuerySnapshot
            lateinit var templates: QuerySnapshot

            awaitAll(
                    async { userPrefs = userPrefsRef.get().await() },
                    async { teams = teamsQuery.get().await() },
                    async { templates = getTemplatesQuery().get().await() }
            )

            instance = Instance(
                    prevUid,
                    teams.map { it.reference }.shareTeams(true),
                    templates.map { it.reference }.shareTemplates(true),
                    userPrefs,
                    teams,
                    templates
            )
        }.asTask().continueWith { null }
    }

    override fun onTransferData(response: IdpResponse): Task<Void?>? = GlobalScope.async {
        val (prevUid, teamToken, templateToken, userPrefs, teams, templates) = instance

        for (snapshot in userPrefs) {
            userPrefsRef.document(snapshot.id).set(snapshot.data)
                    .logFailures(snapshot.reference, snapshot.data)
        }

        async {
            val teamRefs = teams.map { it.reference }
            val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, teamToken)

            updateOwner(teamRefs, teamToken, prevUid) { ref ->
                checkNotNull(teams.first { it.reference.path == ref.path }
                                     .getLong(FIRESTORE_NUMBER))
            }
            for (ref in teamRefs) {
                ref.update(tokenPath, FieldValue.delete()).logFailures(ref, teamToken)
            }
        }.logFailures()

        async {
            val templateRefs = templates.map { it.reference }
            val tokenPath = FieldPath.of(FIRESTORE_ACTIVE_TOKENS, templateToken)

            updateOwner(templateRefs, templateToken, prevUid) { ref ->
                checkNotNull(templates.first { it.reference.path == ref.path }
                                     .getDate(FIRESTORE_TIMESTAMP))
            }
            for (ref in templateRefs) {
                ref.update(tokenPath, FieldValue.delete()).logFailures(ref, templateToken)
            }
        }.logFailures()

        null
    }.asTask()

    /** Stores an immutable representation of the current invocation to prevent race conditions. */
    data class Instance(
            val prevUid: String,

            val teamToken: String,
            val templateToken: String,

            val userPrefs: QuerySnapshot,
            val teams: QuerySnapshot,
            val templates: QuerySnapshot
    )
}
