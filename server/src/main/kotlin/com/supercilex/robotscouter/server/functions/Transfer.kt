package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.common.FIRESTORE_OWNERS
import com.supercilex.robotscouter.common.FIRESTORE_PREV_UID
import com.supercilex.robotscouter.common.FIRESTORE_REF
import com.supercilex.robotscouter.common.FIRESTORE_TOKEN
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.server.utils.FieldValue
import com.supercilex.robotscouter.server.utils.batch
import com.supercilex.robotscouter.server.utils.firestore
import com.supercilex.robotscouter.server.utils.types.CallableContext
import com.supercilex.robotscouter.server.utils.types.HttpsError
import kotlin.js.Json
import kotlin.js.Promise

fun updateOwners(data: Json, context: CallableContext): Promise<*>? {
    val auth = context.auth
    val token = data[FIRESTORE_TOKEN] as? String
    val path = data[FIRESTORE_REF] as? String
    val prevUid = data[FIRESTORE_PREV_UID]
    val value = data[FIRESTORE_VALUE]

    if (auth == null) throw HttpsError("unauthenticated")
    if (token == null || path == null || value == null) throw HttpsError("invalid-argument")
    if (prevUid != null) {
        if (prevUid !is String) {
            throw HttpsError("invalid-argument")
        } else if (prevUid == auth.uid) {
            throw HttpsError("already-exists", "Cannot add and remove the same user")
        }
    }
    prevUid as String?

    val ref = firestore.doc(path)
    val oldOwnerPath = prevUid?.let { "$FIRESTORE_OWNERS.$it" }
    val newOwnerPath = "$FIRESTORE_OWNERS.${auth.uid}"

    return ref.get().then {
        if (!it.exists) throw HttpsError("not-found")

        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        if ((it.get(FIRESTORE_ACTIVE_TOKENS) as Json)[token] == null) {
            throw HttpsError("permission-denied", "Token $token is invalid for $path")
        }
    }.then {
        firestore.batch {
            oldOwnerPath?.let { update(ref, it, FieldValue.delete()) }
            update(ref, newOwnerPath, value)
        }
    }
}
