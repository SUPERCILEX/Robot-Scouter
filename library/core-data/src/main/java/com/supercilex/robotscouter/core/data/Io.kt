package com.supercilex.robotscouter.core.data

import android.webkit.MimeTypeMap
import java.io.File

/** @return this directory after ensuring that it either already exists or was created */
fun File.safeMkdirs(): File = apply {
    val create = { exists() || mkdirs() }
    check(create() || create()) { "Unable to create $this" }
}

/** @return this file after ensuring that it either already exists or was created */
fun File.safeCreateNewFile(): File = apply {
    parentFile?.safeMkdirs()

    val create = { exists() || createNewFile() }
    check(create() || create()) { "Unable to create $this" }
}

fun File.mimeType(): String {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
}
