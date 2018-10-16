package com.supercilex.robotscouter.server.utils

import com.supercilex.robotscouter.common.FIRESTORE_ID
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_POSITION
import com.supercilex.robotscouter.common.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_UNIT
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import com.supercilex.robotscouter.common.isPolynomial
import kotlin.js.Json
import kotlin.js.json

@DslMarker
annotation class MetricBuilder

fun metrics(builder: Metrics.() -> Unit) = Metrics().apply(builder).toJson()

fun Metrics.header(id: String, name: String) = plusAssign(Header(id, name))

fun Metrics.checkbox(id: String, name: String, builder: CheckBox.Builder.() -> Unit = {}) =
        plusAssign(CheckBox.Builder(id, name).apply(builder).build())

fun Metrics.counter(id: String, name: String, builder: Counter.Builder.() -> Unit = {}) =
        plusAssign(Counter.Builder(id, name).apply(builder).build())

fun Metrics.stopwatch(id: String, name: String) = plusAssign(Stopwatch(id, name))

fun Metrics.text(id: String, name: String) = plusAssign(Text(id, name))

fun Metrics.selector(id: String, name: String, builder: Selector.Builder.() -> Unit = {}) =
        plusAssign(Selector.Builder(id, name).apply(builder).build())

@MetricBuilder
class Metrics {
    private val metrics = mutableListOf<Metric>()

    fun toJson(): Json {
        check(metrics.mapTo(mutableSetOf(), Metric::id).size == metrics.size) {
            "Duplicate IDs found"
        }

        return json(*metrics.mapIndexed { index, metric ->
            metric.id to metric.toJson(index)
        }.toTypedArray())
    }

    operator fun plusAssign(metric: Metric) {
        metrics += metric
    }
}

interface Metric {
    val id: String
    val type: Int
    val name: String

    fun toJson(position: Int): Json

    @MetricBuilder
    abstract class BuilderBase<T : Metric>(val id: String, val name: String) {
        abstract fun build(): T
    }
}

abstract class MetricBase(override val type: Int) : Metric

data class Header(
        override val id: String,
        override val name: String
) : MetricBase(5) {
    override fun toJson(position: Int) = json(
            FIRESTORE_TYPE to 5,
            FIRESTORE_NAME to name,
            FIRESTORE_POSITION to position
    )
}

data class CheckBox(
        override val id: String,
        override val name: String,
        val checked: Boolean
) : MetricBase(0) {
    override fun toJson(position: Int) = json(
            FIRESTORE_TYPE to 0,
            FIRESTORE_NAME to name,
            FIRESTORE_VALUE to checked,
            FIRESTORE_POSITION to position
    )

    class Builder(
            id: String,
            name: String,
            var checked: Boolean = false
    ) : Metric.BuilderBase<CheckBox>(id, name) {
        override fun build() = CheckBox(id, name, checked)
    }
}

data class Counter(
        override val id: String,
        override val name: String,
        val count: Int,
        val unit: String?
) : MetricBase(1) {
    override fun toJson(position: Int) = json(
            FIRESTORE_TYPE to 1,
            FIRESTORE_NAME to name,
            FIRESTORE_VALUE to count,
            FIRESTORE_UNIT to unit,
            FIRESTORE_POSITION to position
    )

    class Builder(
            id: String,
            name: String,
            var count: Int = 0,
            var unit: String? = null
    ) : Metric.BuilderBase<Counter>(id, name) {
        override fun build() = Counter(id, name, count, unit)
    }
}

data class Stopwatch(
        override val id: String,
        override val name: String
) : MetricBase(4) {
    override fun toJson(position: Int) = json(
            FIRESTORE_TYPE to 4,
            FIRESTORE_NAME to name,
            FIRESTORE_VALUE to emptyArray<Long>(),
            FIRESTORE_POSITION to position
    )
}

data class Text(
        override val id: String,
        override val name: String
) : MetricBase(3) {
    override fun toJson(position: Int) = json(
            FIRESTORE_TYPE to 3,
            FIRESTORE_NAME to name,
            FIRESTORE_POSITION to position
    )
}

data class Selector(
        override val id: String,
        override val name: String,
        val items: List<Item>,
        val defaultItemId: String
) : MetricBase(2) {
    override fun toJson(position: Int) = json(
            FIRESTORE_TYPE to 2,
            FIRESTORE_NAME to name,
            FIRESTORE_VALUE to items.map {
                json(FIRESTORE_ID to it.id, FIRESTORE_NAME to it.name)
            }.toTypedArray(),
            FIRESTORE_SELECTED_VALUE_ID to defaultItemId,
            FIRESTORE_POSITION to position
    )

    data class Item(val id: String, val name: String)

    class Builder(id: String, name: String) : Metric.BuilderBase<Selector>(id, name) {
        private val items = mutableListOf<Item>()
        private var defaultItemId: String? = null

        @Suppress("FunctionName") // Fake class
        fun Item(id: String, name: String, default: Boolean = false): Item {
            if (default) {
                check(defaultItemId == null) { "Cannot have multiple defaults" }
                defaultItemId = id
            }

            return Selector.Item(id, name)
        }

        operator fun Item.unaryPlus() {
            items += this
        }

        override fun build(): Selector {
            check(items.isPolynomial) { "Selectors must have 2 or more items" }
            check(items.mapTo(mutableSetOf(), Item::id).size == items.size) {
                "Duplicate IDs found"
            }

            return Selector(id, name, items, defaultItemId ?: items.first().id)
        }
    }
}
