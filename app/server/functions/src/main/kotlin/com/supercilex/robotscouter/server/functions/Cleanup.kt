package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.FIRESTORE_BASE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_CONTENT_ID
import com.supercilex.robotscouter.common.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.common.FIRESTORE_SCOUT_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_SHARE_TOKEN_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_SHARE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TEAM_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TEMPLATE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
import com.supercilex.robotscouter.server.utils.FIRESTORE_EMAIL
import com.supercilex.robotscouter.server.utils.FIRESTORE_PHONE_NUMBER
import com.supercilex.robotscouter.server.utils.auth
import com.supercilex.robotscouter.server.utils.batch
import com.supercilex.robotscouter.server.utils.delete
import com.supercilex.robotscouter.server.utils.deletionQueue
import com.supercilex.robotscouter.server.utils.duplicateTeams
import com.supercilex.robotscouter.server.utils.firestore
import com.supercilex.robotscouter.server.utils.getTeamsQuery
import com.supercilex.robotscouter.server.utils.getTemplatesQuery
import com.supercilex.robotscouter.server.utils.getTrashedTeamsQuery
import com.supercilex.robotscouter.server.utils.getTrashedTemplatesQuery
import com.supercilex.robotscouter.server.utils.moment
import com.supercilex.robotscouter.server.utils.processInBatches
import com.supercilex.robotscouter.server.utils.teams
import com.supercilex.robotscouter.server.utils.templates
import com.supercilex.robotscouter.server.utils.toMap
import com.supercilex.robotscouter.server.utils.toTeamString
import com.supercilex.robotscouter.server.utils.toTemplateString
import com.supercilex.robotscouter.server.utils.types.Change
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DeltaDocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.FieldValues
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.Timestamp
import com.supercilex.robotscouter.server.utils.userPrefs
import com.supercilex.robotscouter.server.utils.users
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.asPromise
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.await
import kotlinx.coroutines.experimental.awaitAll
import kotlinx.coroutines.experimental.delay
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.Promise

private const val MAX_INACTIVE_USER_DAYS = 365
private const val MAX_INACTIVE_ANONYMOUS_USER_DAYS = 45
private const val TRASH_TIMEOUT_DAYS = 30

fun deleteUnusedData(): Promise<*>? = async {
    console.log("Looking for users that haven't opened Robot Scouter for over a year" +
                        " or anonymous users that haven't opened Robot Scouter in over 60 days.")

    val fullUser = async {
        deleteUnusedData(users.where(
                FIRESTORE_LAST_LOGIN,
                "<",
                moment().subtract(MAX_INACTIVE_USER_DAYS, "days").toDate()
        ))
    }
    val anonymousUser = async {
        deleteUnusedData(users.where(
                FIRESTORE_LAST_LOGIN,
                "<",
                moment().subtract(MAX_INACTIVE_ANONYMOUS_USER_DAYS, "days").toDate()
        ).where(
                FIRESTORE_EMAIL, "==", null
        ).where(
                FIRESTORE_PHONE_NUMBER, "==", null
        ))
    }

    awaitAll(fullUser, anonymousUser)
}.asPromise()

fun emptyTrash(): Promise<*>? = async {
    console.log("Emptying trash for all users.")

    deletionQueue.where(
            FIRESTORE_BASE_TIMESTAMP,
            "<",
            moment().subtract(TRASH_TIMEOUT_DAYS, "days").toDate()
    ).processInBatches(10) { processDeletion(it) }
}.asPromise()

fun sanitizeDeletionRequest(event: Change<DeltaDocumentSnapshot>): Promise<*>? {
    fun Json.findOldestDeletionTime(): Date? {
        return Date(sanitizedDeletionRequestData().toMap<Json>().map { (_, data) ->
            data[FIRESTORE_TIMESTAMP].unsafeCast<Timestamp>()
        }.map {
            it.toDate().getTime()
        }.min() ?: return null)
    }

    val snapshot = event.after
    console.log("Sanitizing deletion request for user id ${snapshot.id}.")

    if (!snapshot.exists) return null
    val recalculatedOldestDeletionRequest =
            snapshot.data().findOldestDeletionTime() ?: return snapshot.ref.delete()

    val oldestDeletionRequest =
            snapshot.get(FIRESTORE_BASE_TIMESTAMP).unsafeCast<Timestamp?>()?.toDate() ?: Date(-1)
    return if (oldestDeletionRequest.getTime() != recalculatedOldestDeletionRequest.getTime()) {
        console.log("Updating oldest deletion time to $recalculatedOldestDeletionRequest.")
        snapshot.ref.update(FIRESTORE_BASE_TIMESTAMP, recalculatedOldestDeletionRequest)
    } else {
        null
    }
}

private suspend fun deleteUnusedData(userQuery: Query) = userQuery.processInBatches(10) { user ->
    console.log("Deleting all data for user:\n${JSON.stringify(user.data())}")

    val userId = user.id
    val teams = async {
        val delete: suspend (DocumentSnapshot) -> Unit = {
            it.deleteIfSingleOwner(userId) { deleteTeam(this) }
        }

        getTeamsQuery(userId).processInBatches(action = delete)
        getTrashedTeamsQuery(userId).processInBatches(action = delete)
    }
    val templates = async {
        val delete: suspend (DocumentSnapshot) -> Unit = {
            it.deleteIfSingleOwner(userId) { deleteTemplate(this) }
        }

        getTemplatesQuery(userId).processInBatches(action = delete)
        getTrashedTemplatesQuery(userId).processInBatches(action = delete)
    }

    awaitAll(teams, templates)
    deleteUser(user)

    // Wait because there's a limit of 10 deletions/sec
    delay(1000)
}

private suspend fun processDeletion(request: DocumentSnapshot) {
    val userId = request.id

    val deleteTeam: suspend (id: String) -> Unit = { id ->
        val team = teams.doc(id).get().await()
        if (team.exists) team.deleteIfSingleOwner(userId) { deleteTeam(this) }
    }

    val deleteScout: suspend (teamId: String, scoutId: String) -> Unit = { teamId, scoutId ->
        val scout = teams.doc(teamId).collection(FIRESTORE_SCOUTS).doc(scoutId)

        console.log("Deleting scout: ${scout.id}")
        scout.collection(FIRESTORE_METRICS).delete()
        scout.delete().await()
    }

    val deleteTemplate: suspend (id: String) -> Unit = { id ->
        val template = templates.doc(id).get().await()
        if (template.exists) template.deleteIfSingleOwner(userId) { deleteTemplate(this) }
    }

    val deleteShareToken: suspend (data: Json, token: String) -> Unit = { data, token ->
        fun CollectionReference.deletions(): List<Deferred<*>> {
            @Suppress("UNCHECKED_CAST") // We know its type
            val ids = data[FIRESTORE_CONTENT_ID] as Array<String>
            return ids.map {
                async {
                    val content = doc(it).get().await()
                    if (content.exists) {
                        content.ref.update(
                                "$FIRESTORE_ACTIVE_TOKENS.$token",
                                FieldValues.delete()
                        ).await()
                    }
                }
            }
        }

        console.log("Deleting share token: $token")
        when (data[FIRESTORE_SHARE_TYPE] as Int) {
            FIRESTORE_TEAM_TYPE -> teams.deletions()
            FIRESTORE_TEMPLATE_TYPE -> templates.deletions()
            else -> error("Unknown share type: ${data[FIRESTORE_SHARE_TYPE]}")
        }.awaitAll()
    }

    val requests = request.data().sanitizedDeletionRequestData()
    val results = requests.toMap<Json>().map { (key, data) ->
        val deletionTime = data[FIRESTORE_TIMESTAMP] as Date
        if ((moment().diff(deletionTime, "days") as Int) < TRASH_TIMEOUT_DAYS) {
            return@map CompletableDeferred(null as String?)
        }

        async {
            when (data[FIRESTORE_TYPE]) {
                FIRESTORE_TEAM_TYPE -> deleteTeam(key)
                FIRESTORE_SCOUT_TYPE -> deleteScout(data[FIRESTORE_CONTENT_ID] as String, key)
                FIRESTORE_TEMPLATE_TYPE -> deleteTemplate(key)
                FIRESTORE_SHARE_TOKEN_TYPE -> deleteShareToken(data, key)
                else -> error("Unknown type: ${data[FIRESTORE_TYPE]}")
            }

            key
        }
    }.awaitAll()

    if (results.none { it == null }) {
        request.ref.delete().await()
    } else {
        firestore.batch {
            for (field in results.filterNotNull()) {
                update(request.ref, field, FieldValues.delete())
            }
        }
    }
}

private suspend fun deleteUser(user: DocumentSnapshot) {
    console.log("Deleting user: ${user.id}")
    try {
        auth.deleteUser(user.id).await()
    } catch (t: Throwable) {
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE") // It's a JS object
        if ((t as Json)["code"] != "auth/user-not-found") throw t
    }

    deletionQueue.doc(user.id).delete().await()
    duplicateTeams.doc(user.id).delete().await()
    user.userPrefs.delete()
    user.ref.delete().await()
}

suspend fun deleteTeam(team: DocumentSnapshot) {
    console.log("Deleting team: ${team.toTeamString()}")
    team.ref.apply {
        collection(FIRESTORE_SCOUTS).delete {
            it.ref.collection(FIRESTORE_METRICS).delete()
        }

        team.get<Json>(FIRESTORE_OWNERS).toMap<Long>().map { (uid) ->
            duplicateTeams.doc(uid).update(id, FieldValues.delete())
        }.forEach { it.await() }

        delete().await()
    }
}

private suspend fun deleteTemplate(template: DocumentSnapshot) {
    console.log("Deleting template: ${template.toTemplateString()}")
    template.ref.apply {
        collection(FIRESTORE_METRICS).delete()
        delete().await()
    }
}

private suspend fun DocumentSnapshot.deleteIfSingleOwner(
        userId: String,
        delete: suspend DocumentSnapshot.() -> Unit
) {
    console.log("Processing deletion request for id $id.")

    val owners = get<Json>(FIRESTORE_OWNERS)
    // language=JavaScript
    if (js("Object.keys(owners).length") as Int > 1) {
        // language=undefined
        console.log("Removing $userId's ownership of ${ref.path}")
        // language=JavaScript
        js("delete owners[userId]")
        ref.update(FIRESTORE_OWNERS, owners).await()
    } else {
        delete(this)
    }
}

private fun Json.sanitizedDeletionRequestData(): Json {
    @Suppress("UNUSED_VARIABLE") // Used in JS
    val requests = this
    // language=JavaScript
    js("delete requests[\"$FIRESTORE_BASE_TIMESTAMP\"]")
    return this
}
