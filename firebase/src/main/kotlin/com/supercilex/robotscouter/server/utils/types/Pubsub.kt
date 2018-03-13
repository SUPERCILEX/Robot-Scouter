@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Json

open external class TopicBuilder {
    open var resource: Any = definedExternally
    open fun onPublish(handler: (event: Event<Message>) -> dynamic /* PromiseLike<Any> | Any */): dynamic = definedExternally
}

open external class Message(data: Any) {
    open var data: String = definedExternally
    open var json: Json = definedExternally
}
