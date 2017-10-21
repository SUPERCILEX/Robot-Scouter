package com.supercilex.robotscouter.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

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
