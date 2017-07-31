package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.FIREBASE_SELECTED_VALUE_KEY
import com.supercilex.robotscouter.util.FIREBASE_UNIT
import com.supercilex.robotscouter.util.FIREBASE_VALUE

sealed class Metric<T>(@Exclude @get:Keep val type: Int, name: String, value: T) {
    class Header(name: String = "") : Metric<Nothing?>(HEADER, name, null)

    class Boolean(name: String = "", value: kotlin.Boolean = false) :
            Metric<kotlin.Boolean>(BOOLEAN, name, value)

    class Number(name: String = "", value: Long = 0, unit: String? = null) :
            Metric<Long>(NUMBER, name, value) {
        @Exclude @get:Keep @set:Keep
        var unit = unit
            set(value) {
                if (field != value) {
                    field = value
                    ref.child(FIREBASE_UNIT).setValue(field)
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

        override fun toString(): String = "${super.toString()}, unit=$unit" // ktlint-disable
    }

    class Stopwatch(name: String = "", value: kotlin.collections.List<Long>? = emptyList()) :
            Metric<kotlin.collections.List<Long>?>(STOPWATCH, name, value)

    class Text(name: String = "", value: String? = null) : Metric<String?>(TEXT, name, value)

    class List(name: String = "",
               value: Map<String, String> = mapOf("a" to "Item 1"),
               selectedValueKey: String? = "a") :
            Metric<Map<String, String>>(LIST, name, value) {
        @Exclude @get:Keep @set:Keep
        var selectedValueKey = selectedValueKey
            set(value) {
                if (field != value) {
                    field = value
                    ref.child(FIREBASE_SELECTED_VALUE_KEY).setValue(field)
                }
            }

        override fun equals(other: Any?): kotlin.Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false
            if (!super.equals(other)) return false

            other as List

            return selectedValueKey == other.selectedValueKey
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (selectedValueKey?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String = "${super.toString()}, selectedValueKey=$selectedValueKey" // ktlint-disable
    }

    @Exclude @get:Exclude @set:Exclude
    lateinit var ref: DatabaseReference

    @Exclude @get:Keep @set:Keep
    var name = name
        set(value) {
            if (field != value) {
                field = value
                ref.child(FIREBASE_NAME).setValue(field)
            }
        }
    @Exclude @get:Keep @set:Keep
    var value = value
        set(value) {
            if (field != value) {
                field = value
                ref.child(FIREBASE_VALUE).setValue(field)
            }
        }

    override fun equals(other: Any?): kotlin.Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Metric<*>

        return type == other.type && ref == other.ref && name == other.name && value == other.value
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + ref.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "${javaClass.simpleName}: ref=$ref, name=\"$name\", value=$value"
}
