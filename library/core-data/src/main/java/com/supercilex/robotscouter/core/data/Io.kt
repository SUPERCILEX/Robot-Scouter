package com.supercilex.robotscouter.core.data

import android.Manifest
import android.os.Build
import android.os.Environment
import android.support.annotation.RequiresPermission
import android.support.annotation.WorkerThread
import java.io.File

const val MIME_TYPE_ANY = "*/*"

val ioPerms = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

private val exports = Environment.getExternalStoragePublicDirectory(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Environment.DIRECTORY_DOCUMENTS
        } else {
            "Documents"
        }
)
private val media: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

@WorkerThread
fun initIo() {
    // Do nothing, this will initialize our static fields
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
