package com.supercilex.robotscouter.core.data

import com.google.firebase.storage.FirebaseStorage
import com.supercilex.robotscouter.core.InvocationMarker
import com.supercilex.robotscouter.core.RobotScouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

abstract class CachingSharer {
    protected suspend fun loadFile(fileName: String) = Dispatchers.IO {
        val shareTemplateFile = File(RobotScouter.cacheDir, fileName)
        if (shareTemplateFile.exists()) {
            val diff = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - shareTemplateFile.lastModified())
            if (diff >= FRESHNESS) {
                if (shareTemplateFile.delete()) {
                    getShareTemplateFromServer(shareTemplateFile)
                } else {
                    throw FileSystemException(
                            shareTemplateFile, reason = "Could not delete old file.")
                }
            } else {
                shareTemplateFile.readText()
            }
        } else {
            getShareTemplateFromServer(shareTemplateFile)
        }.also {
            if (it.isEmpty()) {
                shareTemplateFile.delete()
                throw FileSystemException(shareTemplateFile, reason = "Couldn't load template")
            }
        }
    }

    private suspend fun getShareTemplateFromServer(to: File): String {
        try {
            FirebaseStorage.getInstance().reference.child(to.name).getFile(to).await()
        } catch (e: Exception) {
            throw InvocationMarker(e)
        }

        return to.readText()
    }

    private companion object {
        const val FRESHNESS = 7L
    }
}
