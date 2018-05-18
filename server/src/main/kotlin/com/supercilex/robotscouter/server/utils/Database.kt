package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.WriteBatch
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.await
import kotlinx.coroutines.experimental.awaitAll
import kotlin.js.Json

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

suspend fun Firestore.batch(transaction: WriteBatch.() -> Unit) = batch().run {
    transaction()
    commit().await()
}

suspend fun Query.processInBatches(
        batchSize: Int = 100,
        action: suspend (DocumentSnapshot) -> Unit
) = processInBatches(this, batchSize) {
    it.map { async { action(it) } }.awaitAll()
}

suspend fun CollectionReference.delete(
        batchSize: Int = 100,
        middleMan: suspend (DocumentSnapshot) -> Unit = {}
) = processInBatches(orderBy(FieldPath.documentId()), batchSize) { snapshots ->
    snapshots.map { async { middleMan(it) } }.awaitAll()

    firestore.batch {
        snapshots.forEach { delete(it.ref) }
    }
}

private suspend fun processInBatches(
        query: Query,
        batchSize: Int,
        action: suspend (List<DocumentSnapshot>) -> Unit
) {
    val snapshot = query.limit(batchSize).get().await()
    if (snapshot.docs.isEmpty()) return

    action(snapshot.docs.toList())
    processInBatches(query.startAfter(snapshot.docs.last()), batchSize, action)
}

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
