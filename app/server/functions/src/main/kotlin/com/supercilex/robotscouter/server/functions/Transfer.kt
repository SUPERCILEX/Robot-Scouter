package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.FIRESTORE_MEDIA
import com.supercilex.robotscouter.common.FIRESTORE_METRICS
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_PREV_UID
import com.supercilex.robotscouter.common.FIRESTORE_REF
import com.supercilex.robotscouter.common.FIRESTORE_SCOUTS
import com.supercilex.robotscouter.common.FIRESTORE_TEAMS
import com.supercilex.robotscouter.common.FIRESTORE_TIMESTAMP
import com.supercilex.robotscouter.common.FIRESTORE_TOKEN
import com.supercilex.robotscouter.common.FIRESTORE_WEBSITE
import com.supercilex.robotscouter.common.isPolynomial
import com.supercilex.robotscouter.server.utils.batch
import com.supercilex.robotscouter.server.utils.duplicateTeams
import com.supercilex.robotscouter.server.utils.firestore
import com.supercilex.robotscouter.server.utils.teams
import com.supercilex.robotscouter.server.utils.toMap
import com.supercilex.robotscouter.server.utils.toTeamString
import com.supercilex.robotscouter.server.utils.types.CallableContext
import com.supercilex.robotscouter.server.utils.types.Change
import com.supercilex.robotscouter.server.utils.types.DeltaDocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.FieldValues
import com.supercilex.robotscouter.server.utils.types.HttpsError
import com.supercilex.robotscouter.server.utils.types.SetOptions
import com.supercilex.robotscouter.server.utils.types.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json

fun updateOwners(data: Json, context: CallableContext): Promise<*>? {
    val auth = context.auth
    val token = data[FIRESTORE_TOKEN] as? String
    val path = data[FIRESTORE_REF] as? String
    val prevUid = data[FIRESTORE_PREV_UID]

    if (auth == null) throw HttpsError("unauthenticated")
    if (token == null || path == null) throw HttpsError("invalid-argument")
    if (prevUid != null) {
        if (prevUid !is String) {
            throw HttpsError("invalid-argument")
        } else if (prevUid == auth.uid) {
            throw HttpsError("already-exists", "Cannot add and remove the same user")
        }
    }
    prevUid as String?

    val isTeam = path.contains(FIRESTORE_TEAMS)
    val value = run {
        val number = data[FIRESTORE_NUMBER] as? Number
        val timestamp = data[FIRESTORE_TIMESTAMP] as? Number

        @Suppress("IMPLICIT_CAST_TO_ANY")
        when {
            number != null -> number
            timestamp != null -> Date(timestamp)
            else -> throw HttpsError("invalid-argument")
        }
    }

    val ref = firestore.doc(path)
    val oldOwnerPath = prevUid?.let { "$FIRESTORE_OWNERS.$it" }
    val newOwnerPath = "$FIRESTORE_OWNERS.${auth.uid}"

    return GlobalScope.async {
        val content = ref.get().await()

        if (!content.exists) throw HttpsError("not-found")
        if (content.get<Json>(FIRESTORE_ACTIVE_TOKENS)[token] == null) {
            throw HttpsError("permission-denied", "Token $token is invalid for $path")
        }

        firestore.batch {
            oldOwnerPath?.let {
                update(ref, it, FieldValues.delete())
                if (isTeam) {
                    set(duplicateTeams.doc(prevUid),
                        json(ref.id to FieldValues.delete()),
                        SetOptions.merge)
                }
            }

            update(ref, newOwnerPath, value)
            if (isTeam) {
                set(duplicateTeams.doc(auth.uid),
                    json(ref.id to content.get(FIRESTORE_NUMBER)),
                    SetOptions.merge)
            }
        }
    }.asPromise()
}

fun mergeDuplicateTeams(event: Change<DeltaDocumentSnapshot>): Promise<*>? {
    fun findDups(data: Json) = data.toMap<Int>().toList()
            .groupBy { (_, number) -> number }
            .mapValues { (_, duplicates) -> duplicates.map { (teamId) -> teamId } }
            .filter { (number) -> number >= 0 } // Exclude trashed teams
            .filter { (_, ids) -> ids.isPolynomial }
            .onEach { console.log("Found duplicates: $it") }
            .map { (_, ids) -> ids }

    val snapshot = event.after
    val uid = snapshot.id
    console.log("Checking for duplicate teams for $uid.")

    // Fast paths
    if (!snapshot.exists) return null
    if (findDups(snapshot.data()).isEmpty()) {
        console.log("No duplicates found.")
        return null
    }

    // Slow path
    return firestore.runTransaction t@{ t ->
        CoroutineScope(SupervisorJob()).async {
            val duplicates = findDups(t.get(snapshot.ref).await().data())
            if (duplicates.isEmpty()) return@async

            duplicates.map { ids ->
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                async inner@{
                    val teams = ids.map { teams.doc(it).get() }
                            .map { it.await() }
                            .apply { if (any { !it.exists }) return@inner }
                            .associate { it to it.ref.collection(FIRESTORE_SCOUTS).get() }
                            .mapValues { (_, scout) -> scout.await().docs }
                            .mapValues { (_, scouts) ->
                                scouts.associate { it to it.ref.collection(FIRESTORE_METRICS).get() }
                                        .mapValues { (_, metric) -> metric.await().docs }
                            }
                            .toList()
                            .sortedBy { (team) ->
                                team.get<Timestamp>(FIRESTORE_TIMESTAMP).nanoseconds
                            }

                    val (keep) = teams.first()
                    val merges = teams.subList(1, teams.size)

                    val oldKeepData = keep.data().toMap<Any?>()
                    val newKeepData = oldKeepData.toMutableMap()
                    val newKeepOwners =
                            (newKeepData[FIRESTORE_OWNERS] as Json).toMap<Long>().toMutableMap()

                    for ((merge, scouts) in merges) {
                        val mergeData = merge.data().toMap<Any?>()

                        fun mergeValue(name: String) {
                            if (newKeepData[name] == null) newKeepData[name] = mergeData[name]
                        }
                        mergeValue(FIRESTORE_NAME)
                        mergeValue(FIRESTORE_MEDIA)
                        mergeValue(FIRESTORE_WEBSITE)
                        newKeepOwners.putAll((mergeData[FIRESTORE_OWNERS] as Json).toMap())

                        scouts.map { (scout, metrics) ->
                            async {
                                firestore.batch {
                                    console.log("Copying scout ${scout.ref.path} into team ${keep.id}.")

                                    val ref = keep.ref.collection(FIRESTORE_SCOUTS).doc(scout.id)
                                    set(ref, scout.data())
                                    for (metric in metrics) {
                                        set(ref.collection(FIRESTORE_METRICS).doc(metric.id),
                                            metric.data())
                                    }
                                }
                            }
                        }.awaitAll()
                    }

                    newKeepData[FIRESTORE_OWNERS] = json(*newKeepOwners.toList().toTypedArray())
                    console.log("Updating team to\n$newKeepData\n\nfrom\n$oldKeepData")
                    keep.ref.set(json(*newKeepData.toList().toTypedArray())).await()

                    for ((merge, scouts) in merges) {
                        console.log("Deleting team: ${merge.toTeamString()}")

                        scouts.map { (scout, metrics) ->
                            async {
                                firestore.batch {
                                    for (metric in metrics) delete(metric.ref)
                                    delete(scout.ref)
                                }
                            }
                        }.awaitAll()

                        for ((owner) in merge.get<Json>(FIRESTORE_OWNERS).toMap<Any>()) {
                            t.set(duplicateTeams.doc(owner),
                                  json(merge.id to FieldValues.delete()),
                                  SetOptions.merge)
                        }
                        merge.ref.delete()
                    }
                }
            }.awaitAll()
        }.asPromise()
    }
}
