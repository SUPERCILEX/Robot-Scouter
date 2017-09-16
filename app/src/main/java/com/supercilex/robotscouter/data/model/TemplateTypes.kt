package com.supercilex.robotscouter.data.model

import android.support.annotation.IntDef
import com.supercilex.robotscouter.util.isNumber

const val EMPTY = -1
const val MATCH = 0
const val PIT = 1

const val DEFAULT_TEMPLATE_TYPE = MATCH.toString()
val TEMPLATE_TYPES = listOf(MATCH, PIT)

fun isNativeTemplateType(id: String?): Boolean =
        id?.isNumber() == true && TEMPLATE_TYPES.contains(id.toInt())

@IntDef(EMPTY.toLong(), MATCH.toLong(), PIT.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class TemplateType
