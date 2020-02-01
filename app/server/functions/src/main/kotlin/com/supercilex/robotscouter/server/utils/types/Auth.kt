@file:Suppress(
        "INTERFACE_WITH_SUPERCLASS",
        "OVERRIDING_FINAL_MEMBER",
        "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
        "CONFLICTING_OVERLOADS",
        "unused"
)

package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Promise

external class FunctionsAuth {
    fun user(): UserBuilder = definedExternally
}

external class UserBuilder {
    fun onCreate(handler: (UserInfo) -> Promise<*>?): dynamic = definedExternally
}

external interface UserMetadata {
    val lastSignInTime: String
    val creationTime: String
    fun toJSON(): Any
}

external interface UserInfo {
    val uid: String
    val displayName: String
    val email: String
    val phoneNumber: String
    val photoURL: String
    val providerId: String
    fun toJSON(): Any
}

external interface UserRecord {
    val uid: String
    val email: String?
    val emailVerified: Boolean
    val displayName: String?
    val phoneNumber: String?
    val photoURL: String?
    val disabled: Boolean
    val metadata: UserMetadata
    val providerData: Array<UserInfo>?
    val passwordHash: String? get() = definedExternally
    val passwordSalt: String? get() = definedExternally
    val customClaims: Any? get() = definedExternally
    val tokensValidAfterTime: String? get() = definedExternally
    fun toJSON(): Any
}

external interface ListUsersResult {
    val users: Array<UserRecord>
    val pageToken: String? get() = definedExternally
}

external interface UpdateRequest {
    var displayName: String? get() = definedExternally; set(value) = definedExternally
    var email: String? get() = definedExternally; set(value) = definedExternally
    var emailVerified: Boolean? get() = definedExternally; set(value) = definedExternally
    var phoneNumber: String? get() = definedExternally; set(value) = definedExternally
    var photoURL: String? get() = definedExternally; set(value) = definedExternally
    var disabled: Boolean? get() = definedExternally; set(value) = definedExternally
    var password: String? get() = definedExternally; set(value) = definedExternally
}

external interface CreateRequest : UpdateRequest {
    var uid: String? get() = definedExternally; set(value) = definedExternally
}

external interface Auth {
    var app: App
    fun createCustomToken(uid: String, developerClaims: Any? = definedExternally): Promise<String>
    fun createUser(properties: CreateRequest): Promise<UserRecord>
    fun deleteUser(uid: String): Promise<Unit>
    fun getUser(uid: String): Promise<UserRecord>
    fun getUserByEmail(email: String): Promise<UserRecord>
    fun getUserByPhoneNumber(phoneNumber: String): Promise<UserRecord>
    fun listUsers(maxResults: Number? = definedExternally, pageToken: String? = definedExternally): Promise<ListUsersResult>
    fun updateUser(uid: String, properties: UpdateRequest): Promise<UserRecord>
    fun setCustomUserClaims(uid: String, customUserClaims: Any): Promise<Unit>
    fun revokeRefreshTokens(uid: String): Promise<Unit>
}
