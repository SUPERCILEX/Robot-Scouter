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
