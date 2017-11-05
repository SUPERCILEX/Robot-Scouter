package com.supercilex.robotscouter.data.model

import java.util.Collections

enum class MetricType(val id: Int) {
    HEADER(5),
    BOOLEAN(0),
    NUMBER(1),
    STOPWATCH(4),
    TEXT(3),
    LIST(2);

    companion object {
        /**
         * Identical to the native values() method except that this one returns an immutable [List]
         * instead of an [Array] which must be copied defensively.
         *
         * @see enumValues
         */
        val values: List<MetricType> = Collections.unmodifiableList(values().toList())

        fun valueOf(id: Int): MetricType = requireNotNull(MetricType.values.find { it.id == id }) {
            "Unknown metric type: $id"
        }
    }
}
