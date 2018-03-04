package com.supercilex.robotscouter.util.data

import android.Manifest
import android.os.Environment
import android.support.annotation.RequiresPermission
import android.support.annotation.WorkerThread
import java.io.File
import java.io.IOException

val ioPerms = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

private val exports = // TODO Environment.DIRECTORY_DOCUMENTS can be used after API 19
    File(Environment.getExternalStorageDirectory(), "Documents")
private val media = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)

@WorkerThread
fun createFile(
        prefix: String,
        suffix: String,
        parent: File,
        randomSeparator: String? = System.currentTimeMillis().toString()
): File {
    val tempFile = File(
            parent, "$prefix${if (randomSeparator == null) "" else "_$randomSeparator"}.$suffix")
    return if (tempFile.createNewFile()) tempFile
    else throw IOException("Unable to create temporary file")
}

@get:WorkerThread
@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val exportsFolder: File?
    get() = getFolder(exports)

@get:WorkerThread
@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val mediaFolder: File?
    get() = getFolder(media)

@WorkerThread
fun File.hidden() = File(parentFile, ".$name")

@WorkerThread
@Throws(IOException::class)
fun File.unhide(): File {
    val unhidden = File(parentFile, name.substring(1))
    if (!renameTo(unhidden)) {
        throw IOException("Failed to rename file: $this")
    }
    return unhidden
}

private fun getFolder(folder: File) =
        if (isExternalStorageMounted() && (folder.exists() || folder.mkdirs())) folder else null

private fun isExternalStorageMounted(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
