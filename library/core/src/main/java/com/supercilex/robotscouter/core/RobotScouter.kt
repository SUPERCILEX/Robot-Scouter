package com.supercilex.robotscouter.core

import android.app.Application

@Suppress("PropertyName")
val RobotScouter
    get() = _app
@Suppress("ObjectPropertyName")
var _app: Application by LateinitVal()
