package com.supercilex.robotscouter.util.data

import android.Manifest
import android.os.Environment
import android.support.annotation.RequiresPermission
import java.io.File
import java.io.IOException

val ioPerms = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

private val root = File(Environment.getExternalStorageDirectory(), "Robot Scouter")
private val media = File(root, "Media")

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

@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val rootFolder: File?
    get() = if (isExternalStorageMounted() && (root.exists() || root.mkdirs())) root else null

@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val mediaFolder: File?
    get() = if (rootFolder != null && (media.exists() || media.mkdirs())) media else null

fun File.hide() = File(parentFile, ".$name")

@Throws(IOException::class)
fun File.unhide(): File {
    val unhidden = File(parentFile, name.substring(1))
    if (!renameTo(unhidden)) {
        throw IOException("Failed to rename file: $this")
    }
    return unhidden
}

private fun isExternalStorageMounted(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
