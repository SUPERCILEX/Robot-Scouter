package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.FieldPath
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.WriteBatch
import kotlin.js.Promise

fun DocumentSnapshot.toTeamString() =
        "${data()[FIRESTORE_NUMBER]} - ${data()[FIRESTORE_NAME]}: $id"

fun DocumentSnapshot.toTemplateString() = "${data()[FIRESTORE_NAME]}: $id"

fun Firestore.batch(transaction: WriteBatch.() -> Unit) = batch().run {
    transaction()
    commit()
}

fun CollectionReference.delete(
        batchSize: Int = 100,
        middleMan: (DocumentSnapshot) -> Promise<*> = { Promise.resolve(Unit) }
): Promise<Unit> = deleteQueryBatch(
        firestore,
        orderBy(FieldPath.documentId()).limit(batchSize),
        batchSize,
        middleMan
)

private fun deleteQueryBatch(
        db: Firestore,
        query: Query,
        batchSize: Int,
        middleMan: (DocumentSnapshot) -> Promise<*>
): Promise<Unit> = query.get().then { snapshot ->
    if (snapshot.size == 0) {
        return@then Promise.resolve(0)
    }

    Promise.all(snapshot.docs.map(middleMan).toTypedArray()).then {
        db.batch {
            snapshot.docs.forEach { delete(it.ref) }
        }
    }.then {
        it.size
    }
}.then { numDeleted ->
    if (numDeleted == 0) {
        return@then Promise.resolve(Unit)
    }

    deleteQueryBatch(db, query, batchSize, middleMan)
}.then { Unit }
