package com.supercilex.robotscouter.util

import android.Manifest
import android.os.Environment
import android.support.annotation.RequiresPermission
import java.io.File
import java.io.IOException

val IO_PERMS = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

private val ROOT_FOLDER = File(Environment.getExternalStorageDirectory(), "Robot Scouter")
private val MEDIA_FOLDER = File(ROOT_FOLDER, "Media")

fun createFile(prefix: String,
               suffix: String,
               parent: File,
               randomSeparator: String? = System.currentTimeMillis().toString()): File {
    val tempFile = File(parent, "$prefix${if (randomSeparator == null) "" else "_$randomSeparator"}.$suffix")
    if (tempFile.createNewFile()) {
        return tempFile
    } else {
        throw IOException("Unable to create temporary file")
    }
}

@RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun getRootFolder(): File? =
        if (isExternalStorageMounted() && (ROOT_FOLDER.exists() || ROOT_FOLDER.mkdirs())) ROOT_FOLDER else null

@RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun getMediaFolder(): File? =
        if (getRootFolder() != null && (MEDIA_FOLDER.exists() || MEDIA_FOLDER.mkdirs())) MEDIA_FOLDER else null

fun hideFile(fileName: String): String = ".$fileName"

@Throws(IOException::class)
fun unhideFile(file: File): File {
    val unhidden = File(file.parentFile, file.name.substring(1))
    if (!file.renameTo(unhidden)) {
        throw IOException("Failed to rename file: " + file)
    }
    return unhidden
}

private fun isExternalStorageMounted(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
