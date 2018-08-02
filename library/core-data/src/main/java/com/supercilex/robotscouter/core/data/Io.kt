package com.supercilex.robotscouter.core.data

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import com.supercilex.robotscouter.core.RobotScouter
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

internal val dbCache = File(RobotScouter.cacheDir, "db")
internal val userCache = File(dbCache, "user.json")

@get:WorkerThread
@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val exportsFolder
    get() = exports.get()

@get:WorkerThread
@get:RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
val mediaFolder
    get() = media.get()

@WorkerThread
fun initIo() {
    // Do nothing, this will initialize our static fields
}

fun File.safeMkdirs() = apply {
    check(exists() || mkdirs()) { "Unable to create $this" }
}

fun File.safeCreateNewFile() = apply {
    parentFile.safeMkdirs()
    check(exists() || createNewFile()) { "Unable to create $this" }
}

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

private fun File.get(): File {
    check(Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        "External storage not mounted."
    }
    return this
}
