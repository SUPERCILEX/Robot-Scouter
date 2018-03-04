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
fun initIo() {
    // Do nothing, this will initialize our static fields
}

@WorkerThread
@Throws(IOException::class)
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
fun File.unhidden() = File(parentFile, name.substring(1))

@WorkerThread
fun File.hide(): File? {
    val hidden = hidden()
    return if (!renameTo(hidden)) null else hidden
}

@WorkerThread
fun File.unhide(): File? {
    val unhidden = unhidden()
    return if (!renameTo(unhidden)) null else unhidden
}

private fun getFolder(folder: File) =
        if (isExternalStorageMounted() && (folder.exists() || folder.mkdirs())) folder else null

private fun isExternalStorageMounted(): Boolean =
        Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
