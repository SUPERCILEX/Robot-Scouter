package com.supercilex.robotscouter.build.internal

import child
import org.gradle.api.Task
import org.gradle.api.file.RegularFile

internal val isCi = System.getenv("CI") != null
internal val isMaster = System.getenv("CIRCLE_BRANCH") == "master"
internal val isPr = System.getenv("CIRCLE_PULL_REQUEST") != null
internal val isRelease = /*isMaster && !isPr*/true

internal val Task.secrets: Secrets
    get() {
        val rootProject = project.rootProject
        val android = rootProject.child("android-base").layout.projectDirectory

        val ci = listOf(
                android.file("upload-keystore.jks"),
                android.file("upload-keystore.properties"),
                android.file("google-services.json"),
                android.file("google-play-auto-publisher.json"),
                rootProject.child("core-data").layout.projectDirectory
                        .file("src/main/res/values/config.xml")
        )
        val full = ci + listOf(
                android.file("keystore.jks"),
                android.file("keystore.properties")
        )

        return Secrets(full, ci)
    }

internal data class Secrets(val full: List<RegularFile>, val ci: List<RegularFile>)
