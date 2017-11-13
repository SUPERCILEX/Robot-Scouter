@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "unused")

import kotlin.js.Json

external interface Event<T> {
    var eventId: String? get() = definedExternally; set(value) = definedExternally
    var timestamp: String? get() = definedExternally; set(value) = definedExternally
    var eventType: String? get() = definedExternally; set(value) = definedExternally
    var resource: String? get() = definedExternally; set(value) = definedExternally
    var params: Json? get() = definedExternally; set(value) = definedExternally
    var data: T
}
