package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import com.supercilex.robotscouter.util.FIREBASE_NAME
import com.supercilex.robotscouter.util.FIREBASE_SELECTED_VALUE_KEY
import com.supercilex.robotscouter.util.FIREBASE_UNIT
import com.supercilex.robotscouter.util.FIREBASE_VALUE

// TODO When ScoutListFragmentBase is converted to Kotlin, make is use the default constructor
// params instead of defining its own

sealed class Metric<T>(name: String, value: T) {
    class Boolean(name: String, value: kotlin.Boolean = false) :
            Metric<kotlin.Boolean>(name, value) {
        @Exclude @get:Keep
        override val type = BOOLEAN
    }

    class Number(name: String, value: Long = 0, unit: String? = null) :
            Metric<Long>(name, value) {
        @Exclude @get:Keep
        override val type = NUMBER

        @Exclude @get:Keep @set:Keep
        var unit = unit
            set(value) {
                if (field != value) {
                    field = value
                    ref.child(FIREBASE_UNIT).setValue(field)
                }
            }
    }

    class Stopwatch(name: String, value: kotlin.collections.List<Long>? = null) :
            Metric<kotlin.collections.List<Long>?>(name, value) {
        @Exclude @get:Keep
        override val type = STOPWATCH
    }

    class Text(name: String, value: String? = null) : Metric<String?>(name, value) {
        @Exclude @get:Keep
        override val type = TEXT
    }

    class List(name: String, value: Map<String, String>, selectedValueKey: String? = null) :
            Metric<Map<String, String>>(name, value) {
        @Exclude @get:Keep
        override val type = LIST

        @Exclude @get:Keep @set:Keep
        var selectedValueKey = selectedValueKey
            set(value) {
                if (field != value) {
                    field = value
                    ref.child(FIREBASE_SELECTED_VALUE_KEY).setValue(field)
                }
            }
    }

    class Header(name: String) : Metric<Nothing?>(name, null) {
        @Exclude @get:Keep
        override val type = HEADER
    }


    abstract val type: Int

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

        return if (ref != other.ref) false
        else if (name != other.name) false
        else value == other.value

    }

    override fun hashCode(): Int {
        var result = ref.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "Metric.${javaClass.name}(ref=$ref, name='$name', value=$value)"
}
