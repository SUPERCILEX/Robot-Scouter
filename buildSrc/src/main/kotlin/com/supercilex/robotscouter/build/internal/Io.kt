package com.supercilex.robotscouter.build.internal

import java.io.File

internal fun File.orNull() = takeIf { exists() }
