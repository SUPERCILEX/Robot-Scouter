package com.supercilex.robotscouter.build.internal

import child
import org.gradle.api.Task
import java.io.File

internal val isCi = System.getenv("CI") != null
internal val isMaster = System.getenv("CIRCLE_BRANCH") == "master"
internal val isPr = System.getenv("CIRCLE_PULL_REQUEST") != null
internal val isRelease = isMaster && !isPr

internal val Task.secrets: Secrets
    get() {
        val rootProject = project.rootProject
        val android = rootProject.child("android-base").projectDir

        val ci = listOf(
                File(android, "upload-keystore.jks"),
                File(android, "upload-keystore.properties"),
                File(android, "google-services.json"),
                File(android, "google-play-auto-publisher.json"),
                File(rootProject.child("core-data").projectDir, "src/main/res/values/config.xml")
        )
        val full = ci + listOf(
                File(android, "keystore.jks"),
                File(android, "keystore.properties")
        )

        return Secrets(full, ci)
    }

data class Secrets(val full: List<File>, val ci: List<File>)
