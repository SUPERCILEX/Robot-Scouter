package com.supercilex.robotscouter.util

import java.lang.NumberFormatException

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

fun String.isNumber(): Boolean = try {
    toLong()
    true
} catch (e: NumberFormatException) {
    false
}

fun <T> Array<T>.second(): T {
    if (size < 2) throw NoSuchElementException("List is has less than 2 elements.")
    return this[1]
}

fun <T> List<T>.second(): T {
    if (size < 2) throw NoSuchElementException("List is has less than 2 elements.")
    return this[1]
}
