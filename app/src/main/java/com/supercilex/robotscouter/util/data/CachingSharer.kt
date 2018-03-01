package com.supercilex.robotscouter.util.data

import com.google.firebase.storage.FirebaseStorage
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.await
import java.io.File
import java.util.concurrent.TimeUnit

abstract class CachingSharer {
    protected suspend fun loadFile(fileName: String): String {
        val shareTemplateFile = File(RobotScouter.cacheDir, fileName)
        return if (shareTemplateFile.exists()) {
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
        FirebaseStorage.getInstance().reference.child(to.name).getFile(to).await()
        return to.readText()
    }

    private companion object {
        const val FRESHNESS = 7L
    }
}
