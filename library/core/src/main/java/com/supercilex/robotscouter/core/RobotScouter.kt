package com.supercilex.robotscouter.core

import android.annotation.SuppressLint
import android.content.Context

@Suppress("PropertyName")
val RobotScouter
    get() = _globalContext
@SuppressLint("StaticFieldLeak")
@Suppress("ObjectPropertyName")
lateinit var _globalContext: Context
