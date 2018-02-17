package com.supercilex.robotscouter.util.data

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.second
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

abstract class CachingSharer(private val context: Context) {
    protected fun loadFile(fileName: String) = AsyncTaskExecutor.execute(object : Callable<String> {
        private val tempShareTemplateFile: File
            get() {
                val nameSplit = fileName.split(".")
                return createFile(nameSplit.first(), nameSplit.second(), context.cacheDir, null)
            }

        override fun call(): String {
            val shareTemplateFile = File(context.cacheDir, fileName)
            return if (shareTemplateFile.exists()) {
                val diff = TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - shareTemplateFile.lastModified())
                if (diff >= FRESHNESS) {
                    if (shareTemplateFile.delete()) {
                        getShareTemplateFromServer(tempShareTemplateFile)
                    } else {
                        throw FileSystemException(
                                shareTemplateFile, reason = "Could not delete old file.")
                    }
                } else {
                    shareTemplateFile.readText()
                }
            } else {
                getShareTemplateFromServer(tempShareTemplateFile)
            }.also {
                if (it.isEmpty()) {
                    shareTemplateFile.delete()
                    throw CancellationException("Couldn't load template")
                }
            }
        }

        private fun getShareTemplateFromServer(to: File): String {
            Tasks.await(FirebaseStorage.getInstance().reference.child(fileName).getFile(to))
            return to.readText()
        }
    })

    private companion object {
        const val FRESHNESS = 7L
    }
}
