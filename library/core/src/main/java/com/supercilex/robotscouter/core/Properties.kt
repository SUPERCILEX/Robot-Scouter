package com.supercilex.robotscouter.core

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

class LateinitVal<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = checkNotNull(value) {
        "Property ${property.name} should be initialized before get."
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        check(this.value == null) {
            "Property ${property.name} is a val and cannot change its value."
        }
        this.value = value
    }
}

class ValueSeeker<T>(private val evaluator: () -> T) : ReadOnlyProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>) =
            value ?: synchronized(this) { value ?: evaluator().also { value = it } }
}
