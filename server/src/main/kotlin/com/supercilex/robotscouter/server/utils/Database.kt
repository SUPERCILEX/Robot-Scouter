package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.WriteBatch
import kotlin.js.Json
import kotlin.js.Promise

fun <T> Json.toMap(): Map<String, T> {
    val map: MutableMap<String, T> = mutableMapOf()
    for (key: String in js("Object").keys(this)) {
        @Suppress("UNCHECKED_CAST") // Trust the client
        map[key] = this[key] as T
    }
    return map
}

fun DocumentSnapshot.toTeamString() =
        "${data()[FIRESTORE_NUMBER]} - ${data()[FIRESTORE_NAME]}: $id"

fun DocumentSnapshot.toTemplateString() = "${data()[FIRESTORE_NAME]}: $id"

fun Firestore.batch(transaction: WriteBatch.() -> Unit) = batch().run {
    transaction()
    commit()
}

fun Query.processInBatches(
        batchSize: Int = 100,
        action: (DocumentSnapshot) -> Promise<*>
) = processInBatches(this, batchSize) {
    Promise.all(it.map(action).toTypedArray())
}

fun CollectionReference.delete(
        batchSize: Int = 100,
        middleMan: (DocumentSnapshot) -> Promise<*>? = { null }
) = processInBatches(orderBy(FieldPath.documentId()), batchSize) { snapshots ->
    Promise.all(snapshots.map(middleMan).map {
        it ?: Promise.resolve(Unit)
    }.toTypedArray()).then {
        firestore.batch {
            snapshots.forEach { delete(it.ref) }
        }
    }
}

private fun processInBatches(
        query: Query,
        batchSize: Int,
        action: (List<DocumentSnapshot>) -> Promise<*>
): Promise<Unit> = query.limit(batchSize).get().then { snapshot ->
    if (snapshot.size == 0) return@then Promise.resolve<DocumentSnapshot?>(null)
    action(snapshot.docs.toList()).then { snapshot.docs.last() }
}.then { next ->
    if (next == null) return@then Promise.resolve(Unit)
    processInBatches(query.startAfter(next), batchSize, action)
}.then { Unit }

class FieldValue {
    // language=JavaScript
    companion object {
        fun serverTimestamp(): dynamic =
                js("require(\"firebase-admin\").firestore.FieldValue.serverTimestamp()")

        fun delete(): dynamic =
                js("require(\"firebase-admin\").firestore.FieldValue.delete()")
    }
}

class FieldPath {
    // language=JavaScript
    companion object {
        fun documentId(): dynamic =
                js("require(\"firebase-admin\").firestore.FieldPath.documentId()")
    }
}
