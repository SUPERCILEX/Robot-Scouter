@file:Suppress(
        "INTERFACE_WITH_SUPERCLASS",
        "OVERRIDING_FINAL_MEMBER",
        "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
        "CONFLICTING_OVERLOADS",
        "unused"
)

package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Promise

@Suppress("FunctionName", "UNUSED_PARAMETER", "UNUSED_VARIABLE") // Fake class
fun HttpsError(code: String, message: String? = null, details: Any? = null): Throwable {
    val functions = functions
    return js("new functions.https.HttpsError(code, message, details)")
}

external class Https {
    fun <T> onCall(handler: (data: T, context: CallableContext) -> Promise<*>?): dynamic = definedExternally
}

external interface CallableContext {
    val auth: AuthContext? get() = definedExternally
    val instanceIdToken: String? get() = definedExternally
}
