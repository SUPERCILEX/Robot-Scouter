package com.supercilex.robotscouter.server.utils

import kotlin.js.Json

fun <T> Json.toMap(): Map<String, T> {
    @Suppress("SENSELESS_COMPARISON") // JavaScrip sucks
    if (this == null) return emptyMap()

    val map: MutableMap<String, T> = mutableMapOf()
    for (key: String in js("Object").keys(this)) {
        @Suppress("UNCHECKED_CAST") // Trust the client
        map[key] = this[key] as T
    }
    return map
}
