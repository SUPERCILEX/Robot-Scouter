package com.supercilex.robotscouter.core.data

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import java.io.File

const val MIME_TYPE_ANY = "*/*"

val ioPerms = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

private val exports = Environment.getExternalStoragePublicDirectory(
        if (Build.VERSION.SDK_INT >= 19) {
            Environment.DIRECTORY_DOCUMENTS
        } else {
            "Documents"
        }
)

@get:WorkerThread
@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val exportsFolder
    get() = exports.get()

@WorkerThread
fun initIo() {
    // Do nothing, this will initialize our static fields
}

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

@WorkerThread
fun File.hidden() = File(parentFile, ".$name")

@WorkerThread
fun File.unhidden() = File(parentFile, name.substring(1))

@WorkerThread
fun File.hide() = hidden().takeIf { renameTo(it) }

@WorkerThread
fun File.unhide() = unhidden().takeIf { renameTo(it) }

private fun File.get(): File {
    check(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        "External storage not mounted."
    }
    return this
}
