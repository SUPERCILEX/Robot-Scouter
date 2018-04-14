package com.supercilex.robotscouter.core.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.supercilex.robotscouter.common.FIRESTORE_ID
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.common.FIRESTORE_POSITION
import com.supercilex.robotscouter.common.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
import com.supercilex.robotscouter.common.FIRESTORE_UNIT
import com.supercilex.robotscouter.common.FIRESTORE_VALUE
import kotlin.Boolean as IntrinsicBoolean
import kotlin.collections.List as IntrinsicList

sealed class Metric<T>(
        @Exclude
        @get:Exclude
        val type: MetricType,

        @Exclude
        @get:Keep
        var name: String,

        @Exclude
        @get:Keep
        var value: T,

        @Exclude
        @get:Keep
        override var position: Int,

        @Exclude
        @get:Exclude
        override val ref: DocumentReference
) : OrderedRemoteModel {
    class Header(
            name: String = "",
            position: Int,
            ref: DocumentReference
    ) : Metric<Nothing?>(MetricType.HEADER, name, null, position, ref)

    class Boolean(
            name: String = "",
            value: IntrinsicBoolean = false,
            position: Int,
            ref: DocumentReference
    ) : Metric<IntrinsicBoolean>(MetricType.BOOLEAN, name, value, position, ref)

    class Number(
            name: String = "",
            value: Long = 0,

            @Exclude
            @get:Keep
            var unit: String? = null,

            position: Int,
            ref: DocumentReference
    ) : Metric<Long>(MetricType.NUMBER, name, value, position, ref) {
        override fun equals(other: Any?): IntrinsicBoolean {
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
            value: IntrinsicList<Long> = emptyList(),
            position: Int,
            ref: DocumentReference
    ) : Metric<IntrinsicList<Long>>(MetricType.STOPWATCH, name, value, position, ref)

    class Text(
            name: String = "",
            value: String? = null,
            position: Int,
            ref: DocumentReference
    ) : Metric<String?>(MetricType.TEXT, name, value, position, ref)

    class List(
            name: String = "",
            value: IntrinsicList<Item> = emptyList(),

            @Exclude
            @get:Keep
            var selectedValueId: String? = null,

            position: Int,
            ref: DocumentReference
    ) : Metric<IntrinsicList<List.Item>>(MetricType.LIST, name, value, position, ref) {

        override fun equals(other: Any?): IntrinsicBoolean {
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

        data class Item(
                @Exclude
                @get:Keep
                val id: String,

                @Exclude
                @get:Keep
                val name: String
        )
    }

    @get:PropertyName(FIRESTORE_TYPE)
    @get:Keep
    val id
        get() = type.id

    override fun equals(other: Any?): IntrinsicBoolean {
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

    override fun toString(): String = "${javaClass.simpleName}: " +
            "ref=${ref.path}, position=$position, name=\"$name\", value=$value"

    companion object {
        @Suppress("UNCHECKED_CAST") // We know what our data types are
        fun parse(fields: Map<String, Any?>, ref: DocumentReference): Metric<*> {
            val position = (fields[FIRESTORE_POSITION] as Long).toInt()
            val type = (fields[FIRESTORE_TYPE] as Long).toInt()
            val name = (fields[FIRESTORE_NAME] as String?).orEmpty()

            return when (MetricType.valueOf(type)) {
                MetricType.HEADER -> Header(name, position, ref)
                MetricType.BOOLEAN ->
                    Boolean(name, fields[FIRESTORE_VALUE] as IntrinsicBoolean, position, ref)
                MetricType.NUMBER -> Number(
                        name,
                        fields[FIRESTORE_VALUE] as Long,
                        fields[FIRESTORE_UNIT] as String?,
                        position,
                        ref
                )
                MetricType.STOPWATCH -> Stopwatch(
                        name,
                        fields[FIRESTORE_VALUE] as IntrinsicList<Long>,
                        position,
                        ref
                )
                MetricType.TEXT -> Text(
                        name,
                        fields[FIRESTORE_VALUE] as String?,
                        position,
                        ref
                )
                MetricType.LIST -> List(
                        name,
                        try {
                            fields[FIRESTORE_VALUE] as IntrinsicList<Map<String, String>>
                        } catch (e: ClassCastException) {
                            // TODO remove at some point, used to support old model
                            (fields[FIRESTORE_VALUE] as Map<String, String>).map {
                                mapOf(
                                        FIRESTORE_ID to it.key,
                                        FIRESTORE_NAME to (it.value as String?).toString()
                                )
                            }
                        }.map {
                            List.Item(it[FIRESTORE_ID] as String, it[FIRESTORE_NAME] as String)
                        },
                        fields[FIRESTORE_SELECTED_VALUE_ID] as String?,
                        position,
                        ref
                )
            }
        }
    }
}
