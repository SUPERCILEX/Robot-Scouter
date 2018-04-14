package com.supercilex.robotscouter.core.data

val Iterable<*>.isSingleton: Boolean
    get() = iterator().let {
        if (!it.hasNext()) return false
        it.next()
        return !it.hasNext()
    }

val Iterable<*>.isPolynomial: Boolean
    get() = iterator().let {
        return if (!it.hasNext()) false else !isSingleton
    }

fun <T> Array<T>.second(): T {
    if (size < 2) throw NoSuchElementException("List has less than 2 elements.")
    return this[1]
}

fun <T> List<T>.second(): T {
    if (size < 2) throw NoSuchElementException("List has less than 2 elements.")
    return this[1]
}
