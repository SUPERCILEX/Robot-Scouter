package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.WriteBatch
import com.supercilex.robotscouter.util.FIRESTORE_ID
import com.supercilex.robotscouter.util.FIRESTORE_NAME
import com.supercilex.robotscouter.util.FIRESTORE_POSITION
import com.supercilex.robotscouter.util.FIRESTORE_SELECTED_VALUE_ID
import com.supercilex.robotscouter.util.FIRESTORE_TYPE
import com.supercilex.robotscouter.util.FIRESTORE_UNIT
import com.supercilex.robotscouter.util.FIRESTORE_VALUE
import com.supercilex.robotscouter.util.log
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logUpdate

private typealias IntrinsicBoolean = Boolean
private typealias IntrinsicList <T> = List<T>

sealed class Metric<T>(
        @Exclude
        @get:Exclude
        val type: MetricType,

        name: String,
        value: T,

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
            unit: String? = null,
            position: Int,
            ref: DocumentReference
    ) : Metric<Long>(MetricType.NUMBER, name, value, position, ref) {
        @Exclude
        @get:Keep
        var unit = unit
            set(value) {
                if (field != value) {
                    field = value
                    ref.log().update(FIRESTORE_UNIT, field).logFailures()
                }
            }

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
            selectedValueId: String? = null,
            position: Int,
            ref: DocumentReference
    ) : Metric<IntrinsicList<List.Item>>(MetricType.LIST, name, value, position, ref) {
        @get:Exclude
        @set:Exclude
        override var value: IntrinsicList<Item>
            get() = super.value
            set(value) = updateValue(value)

        @Exclude
        @get:Exclude
        private var _selectedValueId = selectedValueId

        @get:Keep
        var selectedValueId
            get() = _selectedValueId
            set(value) = updateSelectedValueId(value)

        @Exclude
        fun updateValue(items: IntrinsicList<Item>, batch: WriteBatch? = null) {
            if (_value == items) return
            _value = items

            batch.update(FIRESTORE_VALUE, items.map {
                mapOf(FIRESTORE_ID to it.id, FIRESTORE_NAME to it.name)
            })
        }

        @Exclude
        fun updateSelectedValueId(id: String?, batch: WriteBatch? = null) {
            if (_selectedValueId == id) return
            _selectedValueId = id

            logUpdate()
            batch.update(FIRESTORE_SELECTED_VALUE_ID, id as Any)
        }

        private fun WriteBatch?.update(id: String, o: Any) {
            if (this == null) {
                ref.log().update(id, o).logFailures()
            } else {
                update(ref.log(), id, o)
            }
        }

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

    @Exclude
    @get:Keep
    var name = name
        set(value) {
            if (field != value) {
                field = value
                ref.log().update(FIRESTORE_NAME, field).logFailures()
            }
        }

    @Exclude
    @get:Exclude
    protected var _value = value

    @get:Keep
    open var value
        get() = _value
        set(value) {
            if (_value != value) {
                _value = value
                logUpdate()
                ref.log().update(FIRESTORE_VALUE, _value).logFailures()
            }
        }

    override fun equals(other: Any?): IntrinsicBoolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Metric<*>

        return type == other.type && position == other.position && ref.path == other.ref.path
                && name == other.name && _value == other._value
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + position
        result = 31 * result + ref.path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (_value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "${javaClass.simpleName}: " +
            "ref=${ref.path}, position=$position, name=\"$name\", value=$_value"

    companion object {
        @Suppress("UNCHECKED_CAST") // We know what our data types are
        fun parse(fields: Map<String, Any?>, ref: DocumentReference): Metric<*> {
            val position = (fields[FIRESTORE_POSITION] as Long).toInt()
            val type = (fields[FIRESTORE_TYPE] as Long).toInt()
            val name = (fields[FIRESTORE_NAME] as String?).orEmpty()

            return when (MetricType.valueOf(type)) {
                MetricType.HEADER -> Metric.Header(name, position, ref)
                MetricType.BOOLEAN -> {
                    Metric.Boolean(name, fields[FIRESTORE_VALUE] as IntrinsicBoolean, position, ref)
                }
                MetricType.NUMBER -> Metric.Number(
                        name,
                        fields[FIRESTORE_VALUE] as Long,
                        fields[FIRESTORE_UNIT] as String?,
                        position,
                        ref
                )
                MetricType.STOPWATCH -> Metric.Stopwatch(
                        name,
                        fields[FIRESTORE_VALUE] as IntrinsicList<Long>,
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
