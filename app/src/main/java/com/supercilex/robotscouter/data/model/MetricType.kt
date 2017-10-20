package com.supercilex.robotscouter.data.model

enum class MetricType(val id: Int) {
    HEADER(5),
    BOOLEAN(0),
    NUMBER(1),
    STOPWATCH(4),
    TEXT(3),
    LIST(2);

    companion object {
        fun valueOf(id: Int): MetricType {
            return MetricType.values().find { it.id == id }
                    ?: throw IllegalArgumentException("Unknown metric type: $id")
        }
    }
}
