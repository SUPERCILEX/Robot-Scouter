@file:Suppress(
        "INTERFACE_WITH_SUPERCLASS",
        "OVERRIDING_FINAL_MEMBER",
        "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
        "CONFLICTING_OVERLOADS",
        "unused"
)

package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Json
import kotlin.js.Promise

@Suppress("FunctionName", "UNUSED_PARAMETER", "UNUSED_VARIABLE") // Fake class
fun HttpsError(code: String, message: String? = null, details: Any? = null): Nothing {
    val functions = functions
    js("throw new functions.https.HttpsError(code, message, details)")
    throw Exception() // Never going to get called
}

external class Https {
    fun onCall(handler: (data: Json, context: CallableContext) -> Promise<*>?): dynamic = definedExternally
}

external interface CallableContext {
    val auth: AuthContext? get() = definedExternally
    val instanceIdToken: String? get() = definedExternally
}
