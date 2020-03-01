package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_NUMBER
import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.FieldPaths
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.WriteBatch
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.js.Json

fun DocumentSnapshot.toTeamString() =
        "${data()[FIRESTORE_NUMBER]} - ${data()[FIRESTORE_NAME]}: $id"

fun DocumentSnapshot.toTemplateString() = "${data()[FIRESTORE_NAME]}: $id"

fun <T> DocumentSnapshot.getAsMap(fieldPath: String): Map<String, T> = get<Json>(fieldPath).toMap()

suspend fun Firestore.batch(transaction: WriteBatch.() -> Unit) = batch().run {
    transaction()
    commit().await()
}

suspend fun Query.processInBatches(
        batchSize: Int = 100,
        action: suspend (DocumentSnapshot) -> Unit
) = coroutineScope {
    processInBatches(this@processInBatches, batchSize) {
        it.map { async { action(it) } }.awaitAll()
    }
}

suspend fun CollectionReference.delete(
        batchSize: Int = 100,
        middleMan: suspend (DocumentSnapshot) -> Unit = {}
) = coroutineScope {
    processInBatches(orderBy(FieldPaths.documentId()), batchSize) { snapshots ->
        snapshots.map { async { middleMan(it) } }.awaitAll()

        firestore.batch {
            snapshots.forEach { delete(it.ref) }
        }
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
