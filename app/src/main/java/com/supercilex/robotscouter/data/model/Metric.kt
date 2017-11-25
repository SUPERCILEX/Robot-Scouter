package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.supercilex.robotscouter.util.FIRESTORE_ID
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TYPE
import com.supercilex.robotscouter.util.FIRESTORE_UNIT
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.logUpdate

sealed class Metric<T>(
        @Exclude
        @get:Exclude
        val type: MetricType,

        name: String,
        value: T,

        @Exclude
        @get:Keep
        override var position: Int
) : OrderedRemoteModel {
    class Header(
            name: String = "",
            position: Int
    ) : Metric<Nothing?>(MetricType.HEADER, name, null, position)

    class Boolean(
            name: String = "",
            value: kotlin.Boolean = false,
            position: Int
    ) : Metric<kotlin.Boolean>(MetricType.BOOLEAN, name, value, position)

    class Number(
            name: String = "",
            value: Long = 0, unit: String? = null,
            position: Int
    ) : Metric<Long>(MetricType.NUMBER, name, value, position) {
        @Exclude
        @get:Keep
        var unit = unit
            set(value) {
                if (field != value) {
                    field = value
                    ref.update(FIRESTORE_UNIT, field)
                }
            }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false
            if (!super.equals(other)) return false

            other as Number

            return unit == other.unit
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (unit?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String = "${super.toString()}, unit=$unit"
    }

    class Stopwatch(
            name: String = "",
            value: kotlin.collections.List<Long>? = emptyList(),
            position: Int
    ) : Metric<kotlin.collections.List<Long>?>(MetricType.STOPWATCH, name, value, position)

    class Text(
            name: String = "",
            value: String? = null,
            position: Int
    ) : Metric<String?>(MetricType.TEXT, name, value, position)

    class List(
            name: String = "",
            value: kotlin.collections.List<Item> = emptyList(),
            selectedValueId: String? = null,
            position: Int
    ) : Metric<kotlin.collections.List<List.Item>>(MetricType.LIST, name, value, position) {
        @Exclude
        @get:Keep
        var selectedValueId = selectedValueId
            set(value) {
                if (field != value) {
                    field = value
                    logUpdate()
                    ref.update(FIRESTORE_SELECTED_VALUE_ID, field)
                }
            }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false
            if (!super.equals(other)) return false

            other as List

            return selectedValueId == other.selectedValueId
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (selectedValueId?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String = "${super.toString()}, selectedValueId=$selectedValueId"

        data class Item(val id: String, val name: String)
    }

    @get:PropertyName(FIRESTORE_TYPE)
    @get:Keep
    val id get() = type.id

    @get:Exclude
    override var ref: DocumentReference by LateinitVal()

    @Exclude
    @get:Keep
    var name = name
        set(value) {
            if (field != value) {
                field = value
                ref.update(FIRESTORE_NAME, field)
            }
        }

    @Exclude
    @get:Keep
    var value = value
        set(value) {
            if (field != value) {
                field = value
                logUpdate()
                if (this is List) {
                    // We'd like to just override the setter, but Kotlin doesn't let us do that
                    // which breaks db serialization and causes other problems.
                    ref.update(FIRESTORE_VALUE, this.value.map {
                        mapOf(FIRESTORE_ID to it.id, FIRESTORE_NAME to it.name)
                    })
                } else {
                    ref.update(FIRESTORE_VALUE, field)
                }
            }
        }

    override fun equals(other: Any?): kotlin.Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Metric<*>

        return type == other.type && position == other.position && ref.path == other.ref.path
                && name == other.name && value == other.value
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + position
        result = 31 * result + ref.path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
            "${javaClass.simpleName}: ref=${ref.path}, position=$position, name=\"$name\", value=$value"

    companion object {
        @Suppress("UNCHECKED_CAST") // We know what our data types are
        fun parse(fields: Map<String, Any?>, ref: DocumentReference): Metric<*> {
            val position = (fields[FIRESTORE_POSITION] as Long).toInt()
            val type = (fields[FIRESTORE_TYPE] as Long).toInt()
            val name = (fields[FIRESTORE_NAME] as String?).orEmpty()

            return when (MetricType.valueOf(type)) {
                MetricType.HEADER -> Metric.Header(name, position)
                MetricType.BOOLEAN -> {
                    Metric.Boolean(name, fields[FIRESTORE_VALUE] as kotlin.Boolean, position)
                }
                MetricType.NUMBER -> Metric.Number(
                        name,
                        fields[FIRESTORE_VALUE] as Long,
                        fields[FIRESTORE_UNIT] as String?,
                        position
                )
                MetricType.STOPWATCH -> Metric.Stopwatch(
                        name,
                        fields[FIRESTORE_VALUE] as kotlin.collections.List<Long>,
                        position
                )
                MetricType.TEXT -> Metric.Text(name, fields[FIRESTORE_VALUE] as String?, position)
                MetricType.LIST -> Metric.List(
                        name,
                        try {
                            fields[FIRESTORE_VALUE] as kotlin.collections.List<Map<String, String>>
                        } catch (e: ClassCastException) {
                            // TODO remove at some point, used to support old model
                            (fields[FIRESTORE_VALUE] as kotlin.collections.Map<String, String>).map {
                                mapOf(FIRESTORE_ID to it.key, FIRESTORE_NAME to it.value)
                            }
                        }.map {
                            List.Item(it[FIRESTORE_ID] as String,
                                      it[FIRESTORE_NAME].toString())
                        },
                        fields[FIRESTORE_SELECTED_VALUE_ID] as String?,
                        position
                )
            }.also { it.ref = ref }
        }
    }
}
