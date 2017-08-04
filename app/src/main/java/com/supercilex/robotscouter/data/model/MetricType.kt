package com.supercilex.robotscouter.data.model

import android.support.annotation.IntDef

const val HEADER = 5
const val BOOLEAN = 0
const val NUMBER = 1
const val STOPWATCH = 4
const val TEXT = 3
const val LIST = 2

@IntDef(HEADER.toLong(),
        BOOLEAN.toLong(),
        NUMBER.toLong(),
        STOPWATCH.toLong(),
        TEXT.toLong(),
        LIST.toLong())
@Retention(AnnotationRetention.SOURCE)
annotation class MetricType
