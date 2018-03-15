@file:Suppress(
        "INTERFACE_WITH_SUPERCLASS",
        "OVERRIDING_FINAL_MEMBER",
        "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
        "CONFLICTING_OVERLOADS",
        "unused",
        "PropertyName"
)

package com.supercilex.robotscouter.server.utils.types

import com.supercilex.robotscouter.server.require
import kotlin.js.Json
import kotlin.js.Promise

val functions = require("firebase-functions").unsafeCast<Functions>()
val admin = require("firebase-admin").unsafeCast<Admin>()

external val SDK_VERSION: String = definedExternally
external val apps: Array<App> = definedExternally

external interface Config {
    val firebase: AppOptions
}

external interface AppOptions {
    var credential: Credential? get() = definedExternally; set(value) = definedExternally
    var databaseAuthVariableOverride: Any? get() = definedExternally; set(value) = definedExternally
    var databaseURL: String? get() = definedExternally; set(value) = definedExternally
    var storageBucket: String? get() = definedExternally; set(value) = definedExternally
    var projectId: String? get() = definedExternally; set(value) = definedExternally
}

external interface Credential {
    fun getAccessToken(): Promise<GoogleOAuthAccessToken>
}

external interface GoogleOAuthAccessToken {
    val access_token: String
    val expires_in: Number
}

external interface App {
    val name: String
    val options: AppOptions
    fun firestore(): Firestore
    fun delete(): Promise<Unit>
}

external interface Event<out T> {
    val eventId: String? get() = definedExternally
    val timestamp: String? get() = definedExternally
    val eventType: String? get() = definedExternally
    val resource: String? get() = definedExternally
    val params: Json? get() = definedExternally
    val data: T
}

external class Functions {
    val firestore: NamespaceBuilder = definedExternally
    val pubsub: Pubsub = definedExternally

    fun config(): Config = definedExternally
}

external class Admin {
    fun initializeApp(
            options: AppOptions? = definedExternally,
            name: String? = definedExternally
    ): App = definedExternally

    fun app(name: String? = definedExternally): App = definedExternally
    fun firestore(app: App? = definedExternally): Firestore = definedExternally
}
