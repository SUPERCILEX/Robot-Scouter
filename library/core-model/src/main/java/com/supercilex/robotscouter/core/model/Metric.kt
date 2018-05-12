package com.supercilex.robotscouter.core.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.supercilex.robotscouter.common.FIRESTORE_TYPE
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

        return type == other.type && position == other.position && ref.path == other.ref.path &&
                name == other.name && value == other.value
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
}
