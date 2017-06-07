package com.supercilex.robotscouter.util

import java.lang.NumberFormatException

fun String.isNumber(): Boolean = try {
    toLong(); true
} catch (e: NumberFormatException) {
    false
}
