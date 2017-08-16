package com.supercilex.robotscouter.data.model

import android.support.annotation.IntDef

const val EMPTY = -1
const val MATCH = 0
const val PIT = 1

const val DEFAULT_TEMPLATE_TYPE = MATCH.toString()
val TEMPLATE_TYPES = listOf(MATCH, PIT)

@IntDef(EMPTY.toLong(), MATCH.toLong(), PIT.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class TemplateType
