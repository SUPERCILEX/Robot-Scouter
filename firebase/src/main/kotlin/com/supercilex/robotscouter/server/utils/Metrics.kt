package com.supercilex.robotscouter.server.utils

import kotlin.js.Json
import kotlin.js.json

fun metrics(json: Metrics.() -> Json): Json = Metrics().json()

fun Metrics.header(name: String) = json(
        FIRESTORE_TYPE to 5,
        FIRESTORE_NAME to name,
        FIRESTORE_POSITION to position++
)

fun Metrics.checkbox(name: String, value: Boolean = false) = json(
        FIRESTORE_TYPE to 0,
        FIRESTORE_NAME to name,
        FIRESTORE_VALUE to value,
        FIRESTORE_POSITION to position++
)

fun Metrics.counter(name: String, value: Int = 0, unit: String? = null) = json(
        FIRESTORE_TYPE to 1,
        FIRESTORE_NAME to name,
        FIRESTORE_VALUE to value,
        FIRESTORE_UNIT to unit,
        FIRESTORE_POSITION to position++
)

fun Metrics.stopwatch(name: String, value: List<Long> = emptyList()) = json(
        FIRESTORE_TYPE to 4,
        FIRESTORE_NAME to name,
        FIRESTORE_VALUE to value.toTypedArray(),
        FIRESTORE_POSITION to position++
)

fun Metrics.text(name: String) = json(
        FIRESTORE_TYPE to 3,
        FIRESTORE_NAME to name,
        FIRESTORE_POSITION to position++
)

fun Metrics.selector(name: String, selectedValueId: String, vararg value: ListItem) = json(
        FIRESTORE_TYPE to 2,
        FIRESTORE_NAME to name,
        FIRESTORE_VALUE to json(*value.map { it.id to it.name }.toTypedArray()),
        FIRESTORE_SELECTED_VALUE_ID to selectedValueId,
        FIRESTORE_POSITION to position++
)

class Metrics {
    var position = 0
}

data class ListItem(val id: String, val name: String)
