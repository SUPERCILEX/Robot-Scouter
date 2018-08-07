package com.supercilex.robotscouter.build.internal

import java.io.File

internal fun File.orNull() = if (exists()) this else null
