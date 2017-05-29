package com.supercilex.robotscouter.data.model

import android.support.annotation.IntDef

const val BOOLEAN = 0
const val NUMBER = 1
const val LIST = 2
const val TEXT = 3
const val STOPWATCH = 4
const val HEADER = 5

@IntDef(BOOLEAN.toLong(), NUMBER.toLong(), LIST.toLong(), TEXT.toLong(), STOPWATCH.toLong(), HEADER.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class MetricType
