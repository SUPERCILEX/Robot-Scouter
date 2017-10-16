package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TYPE
import com.supercilex.robotscouter.util.FIRESTORE_UNIT
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.LateinitVal

sealed class Metric<T>(@Exclude @get:Keep val type: Int,
                       name: String,
                       value: T,
                       @Exclude @get:Keep override var position: Int) : OrderedModel {
    class Header(name: String = "", position: Int) : Metric<Nothing?>(HEADER, name, null, position)

    class Boolean(name: String = "", value: kotlin.Boolean = false, position: Int) :
            Metric<kotlin.Boolean>(BOOLEAN, name, value, position)

    class Number(name: String = "", value: Long = 0, unit: String? = null, position: Int) :
            Metric<Long>(NUMBER, name, value, position) {
        @Exclude @get:Keep
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

    class Stopwatch(name: String = "",
                    value: kotlin.collections.List<Long>? = emptyList(),
                    position: Int) :
            Metric<kotlin.collections.List<Long>?>(STOPWATCH, name, value, position)

    class Text(name: String = "", value: String? = null, position: Int) :
            Metric<String?>(TEXT, name, value, position)

    class List(name: String = "",
               value: Map<String, String> = mapOf("a" to "Item 1"),
               selectedValueId: String? = "a",
               position: Int) :
            Metric<Map<String, String>>(LIST, name, value, position) {
        @Exclude @get:Keep
        var selectedValueId = selectedValueId
            set(value) {
                if (field != value) {
                    field = value
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
    }

    @get:Exclude
    override var ref: DocumentReference by LateinitVal()

    @Exclude @get:Keep
    var name = name
        set(value) {
            if (field != value) {
                field = value
                ref.update(FIRESTORE_NAME, field)
            }
        }
    @Exclude @get:Keep
    var value = value
        set(value) {
            if (field != value) {
                field = value
                ref.update(FIRESTORE_VALUE, field)
            }
        }

    override fun equals(other: Any?): kotlin.Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Metric<*>

        return type == other.type && ref.path == other.ref.path
                && name == other.name && value == other.value
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + ref.path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
            "${javaClass.simpleName}: ref=${ref.path}, position=$position, name=\"$name\", value=$value"

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun parse(fields: Map<String, Any?>, ref: DocumentReference): Metric<*> {
            val position = (fields[FIRESTORE_POSITION] as Long).toInt()
            val type = (fields[FIRESTORE_TYPE] as Long).toInt()
            val name = (fields[FIRESTORE_NAME] as String?).orEmpty()

            return when (type) {
                HEADER -> Metric.Header(name, position)
                BOOLEAN -> Metric.Boolean(name, fields[FIRESTORE_VALUE] as kotlin.Boolean, position)
                NUMBER -> {
                    Metric.Number(
                            name,
                            fields[FIRESTORE_VALUE] as Long,
                            fields[FIRESTORE_UNIT] as String?,
                            position)
                }
                STOPWATCH -> {
                    Metric.Stopwatch(
                            name,
                            fields[FIRESTORE_VALUE] as kotlin.collections.List<Long>,
                            position)
                }
                TEXT -> Metric.Text(name, fields[FIRESTORE_VALUE] as String?, position)
                LIST -> {
                    Metric.List(
                            name,
                            fields[FIRESTORE_VALUE] as Map<String, String>,
                            fields[FIRESTORE_SELECTED_VALUE_ID] as String?,
                            position)
                }
                else -> throw IllegalStateException("Unknown metric type: $type")
            }.also { it.ref = ref }
        }
    }
}
