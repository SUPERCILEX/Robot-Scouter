package com.supercilex.robotscouter.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LateinitVal<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            value ?: throw IllegalStateException("Property ${property.name} should be initialized before get.")

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (this.value != null) throw IllegalStateException(
                "Property ${property.name} is a val and cannot change its value.")
        this.value = value
    }
}
