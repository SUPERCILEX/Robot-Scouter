package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.DeletionType
import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.FIRESTORE_BASE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_CONTENT_ID
import com.supercilex.robotscouter.common.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.common.FIRESTORE_SHARE_TYPE
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
import com.supercilex.robotscouter.server.utils.types.CallableContext
import com.supercilex.robotscouter.server.utils.types.Change
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DeltaDocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.FieldValues
import com.supercilex.robotscouter.server.utils.types.HttpsError
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.SetOptions
import com.supercilex.robotscouter.server.utils.types.Timestamp
import com.supercilex.robotscouter.server.utils.types.Timestamps
import com.supercilex.robotscouter.server.utils.userPrefs
import com.supercilex.robotscouter.server.utils.users
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asDeferred
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

private const val MAX_INACTIVE_USER_DAYS = 365
private const val MAX_INACTIVE_ANONYMOUS_USER_DAYS = 45
private const val TRASH_TIMEOUT_DAYS = 30

fun deleteUnusedData(): Promise<*>? = GlobalScope.async {
    console.log("Looking for users that haven't opened Robot Scouter for over a year" +
                        " or anonymous users that haven't opened Robot Scouter in over 60 days.")

    val fullUser = async {
        deleteUnusedData(users.where(
                FIRESTORE_LAST_LOGIN,
                "<",
                Timestamps.fromDate(moment().subtract(MAX_INACTIVE_USER_DAYS, "days").toDate())
        ))
    }
    val anonymousUser = async {
        deleteUnusedData(users.where(
                FIRESTORE_LAST_LOGIN,
                "<",
                Timestamps.fromDate(
                        moment().subtract(MAX_INACTIVE_ANONYMOUS_USER_DAYS, "days").toDate())
        ).where(
                FIRESTORE_EMAIL, "==", null
        ).where(
                FIRESTORE_PHONE_NUMBER, "==", null
        ))
    }

    awaitAll(fullUser, anonymousUser)
}.asPromise()

fun emptyTrash(): Promise<*>? = GlobalScope.async {
    console.log("Emptying trash for all users.")

    deletionQueue.where(
            FIRESTORE_BASE_TIMESTAMP,
            "<",
            Timestamps.fromDate(moment().subtract(TRASH_TIMEOUT_DAYS, "days").toDate())
    ).processInBatches(10) { processDeletion(it) }
}.asPromise()

fun emptyTrash(data: Array<String>?, context: CallableContext): Promise<*>? {
    val auth = context.auth ?: throw HttpsError("unauthenticated")

    console.log("Emptying trash for ${auth.uid}.")
    return GlobalScope.async {
        val requests = deletionQueue.doc(auth.uid).get().await()

        if (!requests.exists) {
            console.log("Nothing to delete")
            return@async
        }

        processDeletion(requests, data.orEmpty().toList())
    }.asPromise()
}

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

private suspend fun CoroutineScope.deleteUnusedData(
        userQuery: Query
) = userQuery.processInBatches(10) { user ->
    console.log("Deleting all data for user:\n${JSON.stringify(user.data())}")

    val userId = user.id
    val teams = async {
        val delete: suspend DocumentSnapshot.() -> Unit = {
            deleteIfSingleOwner(userId) { deleteTeam(it) }
        }

        getTeamsQuery(userId).processInBatches(action = delete)
        getTrashedTeamsQuery(userId).processInBatches(action = delete)
    }
    val templates = async {
        val delete: suspend DocumentSnapshot.() -> Unit = {
            deleteIfSingleOwner(userId) { deleteTemplate(it) }
        }

        getTemplatesQuery(userId).processInBatches(action = delete)
        getTrashedTemplatesQuery(userId).processInBatches(action = delete)
    }

    awaitAll(teams, templates)
    deleteUser(user)

    // Wait because there's a limit of 10 deletions/sec
    delay(1000)
}

/**
 * Deletes data referenced in [request].
 *
 * @param ids If unspecified, items are only deleted after [TRASH_TIMEOUT_DAYS]. If specified but
 *            empty, all data is deleted. Otherwise, only the specified items are deleted.
 * @see deletionQueue
 */
private suspend fun CoroutineScope.processDeletion(
        request: DocumentSnapshot,
        ids: List<String>? = null
) {
    val userId = request.id

    suspend fun deleteTeam(id: String) {
        val team = teams.doc(id).get().await()
        if (team.exists) team.deleteIfSingleOwner(userId) { deleteTeam(it) }
    }

    suspend fun deleteScout(teamId: String, scoutId: String) {
        val scout = teams.doc(teamId).collection(FIRESTORE_SCOUTS).doc(scoutId)

        console.log("Deleting scout: ${scout.id}")
        scout.collection(FIRESTORE_METRICS).delete()
        scout.delete().await()
    }

    suspend fun deleteTemplate(id: String) {
        val template = templates.doc(id).get().await()
        if (template.exists) template.deleteIfSingleOwner(userId) { deleteTemplate(it) }
    }

    suspend fun deleteShareToken(data: Json, token: String) {
        suspend fun CollectionReference.deleteAll() {
            @Suppress("UNCHECKED_CAST") // We know its type
            val backingIds = data[FIRESTORE_CONTENT_ID] as Array<String>
            backingIds.map {
                launch {
                    val content = doc(it).get().await()
                    if (content.exists) {
                        content.ref.update(
                                "$FIRESTORE_ACTIVE_TOKENS.$token",
                                FieldValues.delete()
                        ).await()
                    }
                }
            }.joinAll()
        }

        console.log("Deleting share token: $token")
        when (val type = DeletionType.valueOf(data[FIRESTORE_SHARE_TYPE] as Int)) {
            DeletionType.TEAM -> teams.deleteAll()
            DeletionType.TEMPLATE -> templates.deleteAll()
            else -> error("Unsupported share type: $type")
        }
    }

    val requests = request.data().sanitizedDeletionRequestData()
    val results = requests.toMap<Json>().map { (key, data) ->
        val deletionTime = data[FIRESTORE_TIMESTAMP].unsafeCast<Timestamp>()
        if (
            (moment().diff(deletionTime.toDate(), "days") as Int) < TRASH_TIMEOUT_DAYS &&
            (ids == null || ids.isNotEmpty() && !ids.contains(key))
        ) return@map CompletableDeferred(null as String?)

        async {
            when (DeletionType.valueOf(data[FIRESTORE_TYPE] as Int)) {
                DeletionType.TEAM -> deleteTeam(key)
                DeletionType.SCOUT -> deleteScout(data[FIRESTORE_CONTENT_ID] as String, key)
                DeletionType.TEMPLATE -> deleteTemplate(key)
                DeletionType.SHARE_TOKEN -> deleteShareToken(data, key)
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
            duplicateTeams.doc(uid)
                    .set(json(id to FieldValues.delete()), SetOptions.merge)
                    .asDeferred()
        }.awaitAll()

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
        delete: suspend (DocumentSnapshot) -> Unit
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
