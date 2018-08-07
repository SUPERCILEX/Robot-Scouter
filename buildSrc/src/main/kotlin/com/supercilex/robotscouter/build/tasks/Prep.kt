package com.supercilex.robotscouter.build.tasks

import child
import com.google.common.io.Files
import com.supercilex.robotscouter.build.internal.isRelease
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class DeployAndroidPrep : DefaultTask() {
    @TaskAction
    fun moveFiles() {
        check(isRelease) { "This action cannot be performed in a dev build." }

        val projectDir = project.child("android-base").projectDir

        // Prep for Bundle generation from upload signing key for publishing
        Files.move(
                File(projectDir, "upload-keystore.jks"),
                File(projectDir, "keystore.jks")
        )
        Files.move(
                File(projectDir, "upload-keystore.properties"),
                File(projectDir, "keystore.properties")
        )
    }
}

open class UploadAppToVcPrep : DefaultTask() {
    @TaskAction
    fun moveFiles() {
        check(isRelease) { "This action cannot be performed in a dev build." }

        val buildDir = project.child("android-base").buildDir
        val outer = project.rootDir.parentFile

        // Copy APK generated from real signing key to upload to version history.
        Files.move(
                File(buildDir, "outputs/apk/release/android-base-release.apk"),
                File(outer, "app-base.tmp")
        )
        Files.move(
                File(buildDir, "outputs/bundle/release/android-base.aab"),
                File(outer, "app-release.tmp")
        )
    }
}
