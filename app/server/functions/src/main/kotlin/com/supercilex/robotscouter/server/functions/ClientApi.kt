package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.server.utils.types.CallableContext
import com.supercilex.robotscouter.server.utils.types.HttpsError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlin.js.Json
import kotlin.js.Promise

fun processClientRequest(data: Json, context: CallableContext): Promise<Any?> {
    val auth = context.auth ?: throw HttpsError("unauthenticated")
    val rawOperation = data["operation"] as? String
    console.log(
            "Processing '$rawOperation' operation for user '${auth.uid}' with args: ",
            JSON.stringify(data)
    )

    if (rawOperation == null) {
        throw HttpsError("invalid-argument", "An operation must be supplied.")
    }
    val operation = rawOperation.toUpperCase().replace("-", "_")

    return GlobalScope.async {
        when (operation) {
            "EMPTY_TRASH" -> emptyTrash(auth, data)
            "TRANSFER_USER_DATA" -> transferUserData(auth, data)
            "UPDATE_OWNERS" -> updateOwners(auth, data)
            else -> throw HttpsError("invalid-argument", "Unknown operation: $rawOperation")
        }
    }.asPromise()
}
