package com.supercilex.robotscouter.build.internal

import java.io.File

internal fun File.orNull() = if (exists()) this else null

internal fun File.safeMkdirs() = apply {
    check(exists() || mkdirs()) { "Unable to create $this" }
}

internal fun File.safeCreateNewFile() = apply {
    parentFile.safeMkdirs()
    check(exists() || createNewFile()) { "Unable to create $this" }
}
