package com.supercilex.robotscouter.server.utils

import kotlin.js.Json

fun <T> Json.toMap(): Map<String, T> {
    val map: MutableMap<String, T> = mutableMapOf()
    for (key: String in js("Object").keys(this)) {
        @Suppress("UNCHECKED_CAST") // Trust the client
        map[key] = this[key] as T
    }
    return map
}

fun jsObject(init: dynamic.() -> Unit): dynamic {
    val o = js("{}")
    init(o)
    return o
}
