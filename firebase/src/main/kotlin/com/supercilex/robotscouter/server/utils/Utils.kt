package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.server.utils.types.CollectionReference
import com.supercilex.robotscouter.server.utils.types.DocumentSnapshot
import com.supercilex.robotscouter.server.utils.types.Firestore
import com.supercilex.robotscouter.server.utils.types.Query
import com.supercilex.robotscouter.server.utils.types.WriteBatch
import kotlin.js.Promise

inline fun Firestore.firestoreBatch(transaction: WriteBatch.() -> Unit) = batch().run {
    transaction()
    commit()
}

fun CollectionReference.delete(batchSize: Int = 100): Promise<List<DocumentSnapshot>> =
        deleteQueryBatch(firestore, orderBy("__name__").limit(batchSize), batchSize)

private fun deleteQueryBatch(
        db: Firestore,
        query: Query,
        batchSize: Int,
        deleted: MutableList<DocumentSnapshot> = ArrayList()
): Promise<List<DocumentSnapshot>> = query.get().then {
    if (it.size == 0) {
        return@then Promise.resolve(0)
    }

    db.firestoreBatch {
        it.docs.forEach { delete(it.ref) }
        deleted += it.docs
    }.then {
        it.size
    }
}.then { numDeleted ->
    if (numDeleted == 0) {
        return@then Promise.resolve(deleted)
    }

    deleteQueryBatch(db, query, batchSize, deleted)
}.then { it }.catch {
    emptyList<DocumentSnapshot>()
}
