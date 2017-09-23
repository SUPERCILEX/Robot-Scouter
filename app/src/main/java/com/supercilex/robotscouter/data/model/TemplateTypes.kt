package com.supercilex.robotscouter.data.model

import android.support.annotation.IntDef
import com.supercilex.robotscouter.util.isNumber

const val MATCH = 0
const val PIT = 1
const val EMPTY = 2

const val DEFAULT_TEMPLATE_TYPE = MATCH.toString()
val TEMPLATE_TYPES = listOf(MATCH, PIT, EMPTY)

fun isNativeTemplateType(id: String?): Boolean =
        id?.isNumber() == true && TEMPLATE_TYPES.contains(id.toInt())

@IntDef(MATCH.toLong(), PIT.toLong(), EMPTY.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class TemplateType
