package com.supercilex.robotscouter.build.tasks

import com.supercilex.robotscouter.build.internal.isRelease
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class UploadAppToVcPrep : DefaultTask() {
    @TaskAction
    fun prepFiles() {
        check(isRelease) { "This action cannot be performed in a dev build." }

        val buildDir = project.buildDir
        val outer = project.rootDir.parentFile

        // Copy APK generated from real signing key to upload to version history.
        File(buildDir, "outputs/apk/release/android-base-release.apk")
                .copyTo(File(outer, "app-base.tmp"))
        File(buildDir, "outputs/bundle/release/android-base-release.aab")
                .copyTo(File(outer, "app-release.tmp"))
    }
}
