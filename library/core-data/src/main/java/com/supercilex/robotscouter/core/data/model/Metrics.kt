package com.supercilex.robotscouter.core.data.model

import com.firebase.ui.firestore.SnapshotParser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.common.FIRESTORE_ID
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_POSITION
import com.supercilex.robotscouter.common.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_UNIT
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.core.data.logUpdate
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Metric
import com.supercilex.robotscouter.core.model.MetricType

val metricParser = SnapshotParser { parseMetric(checkNotNull(it.data), it.reference) }

@Suppress("UNCHECKED_CAST") // We know what our data types are
fun parseMetric(fields: Map<String, Any?>, ref: DocumentReference): Metric<*> {
    val position = (fields[FIRESTORE_POSITION] as Long).toInt()
    val type = (fields[FIRESTORE_TYPE] as Long).toInt()
    val name = (fields[FIRESTORE_NAME] as String?).orEmpty()

    return when (MetricType.valueOf(type)) {
        MetricType.HEADER -> Metric.Header(name, position, ref)
        MetricType.BOOLEAN ->
            Metric.Boolean(name, fields[FIRESTORE_VALUE] as Boolean, position, ref)
        MetricType.NUMBER -> Metric.Number(
                name,
                fields[FIRESTORE_VALUE] as Long,
                fields[FIRESTORE_UNIT] as String?,
                position,
                ref
        )
        MetricType.STOPWATCH -> Metric.Stopwatch(
                name,
                fields[FIRESTORE_VALUE] as List<Long>,
                position,
                ref
        )
        MetricType.TEXT -> Metric.Text(
                name,
                fields[FIRESTORE_VALUE] as String?,
                position,
                ref
        )
        MetricType.LIST -> Metric.List(
                name,
                try {
                    fields[FIRESTORE_VALUE] as List<Map<String, String>>
                } catch (e: ClassCastException) {
                    // TODO remove at some point, used to support old model
                    (fields[FIRESTORE_VALUE] as Map<String, String>).map {
                        mapOf(
                                FIRESTORE_ID to it.key,
                                FIRESTORE_NAME to (it.value as String?).toString()
                        )
                    }
                }.map {
                    Metric.List.Item(it[FIRESTORE_ID] as String, it[FIRESTORE_NAME] as String)
                },
                fields[FIRESTORE_SELECTED_VALUE_ID] as String?,
                position,
                ref
        )
    }
}

fun <T> Metric<T>.update(new: T) {
    if (value != new) {
        value = new
        logUpdate()
        ref.update(FIRESTORE_VALUE, new).logFailures(ref, new)
    }
}

fun Metric<*>.updateName(new: String) {
    if (name != new) {
        name = new
        ref.update(FIRESTORE_NAME, new).logFailures(ref, new)
    }
}

fun Metric.Number.updateUnit(new: String?) {
    if (unit != new) {
        unit = new
        ref.update(FIRESTORE_UNIT, new).logFailures(ref, new)
    }
}

@Exclude
fun Metric.List.update(items: List<Metric.List.Item>, batch: WriteBatch? = null) {
    if (value != items) {
        value = items
        update(batch, FIRESTORE_VALUE, items.map {
            mapOf(FIRESTORE_ID to it.id, FIRESTORE_NAME to it.name)
        })
    }
}

@Exclude
fun Metric.List.updateSelectedValueId(new: String?) {
    if (selectedValueId != new) {
        selectedValueId = new
        logUpdate()
        update(null, FIRESTORE_SELECTED_VALUE_ID, new as Any)
    }
}

private fun Metric.List.update(batch: WriteBatch?, id: String, any: Any) {
    if (batch == null) {
        ref.update(id, any).logFailures(ref, "Id: $id, update: $any")
    } else {
        batch.update(ref, id, any)
    }
}
