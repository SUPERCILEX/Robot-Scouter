@file:Suppress(
        "INTERFACE_WITH_SUPERCLASS",
        "OVERRIDING_FINAL_MEMBER",
        "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
        "CONFLICTING_OVERLOADS"
)

package com.supercilex.robotscouter.server.utils.types

import kotlin.js.Json
import kotlin.js.Promise

external class Pubsub {
    fun topic(topic: String): TopicBuilder = definedExternally
}

external class TopicBuilder {
    fun onPublish(handler: (message: Message, context: EventContext) -> Promise<*>?): dynamic = definedExternally
}

external class Message(data: Any) {
    val data: String = definedExternally
    val json: Json = definedExternally
}
