package com.supercilex.robotscouter.core.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.common.FIRESTORE_ID
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.common.FIRESTORE_UNIT
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.core.data.logUpdate
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Metric

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
