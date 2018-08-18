package com.supercilex.robotscouter.build.internal

import child
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File

internal val isCi = System.getenv("CI") != null
internal val isMaster = System.getenv("TRAVIS_BRANCH") == "master"
internal val isPr = System.getenv("TRAVIS_PULL_REQUEST") ?: "false" != "false"
internal val isRelease = isMaster && !isPr

internal val Task.secrets get() = project.secrets
internal val Project.secrets: List<File>
    get() {
        val rootProject = project.rootProject
        val android = rootProject.child("android-base").projectDir

        return listOf(
                File(android, "keystore.jks"),
                File(android, "keystore.properties"),
                File(android, "upload-keystore.jks"),
                File(android, "upload-keystore.properties"),
                File(android, "google-services.json"),
                File(android, "google-play-auto-publisher.json"),
                File(rootProject.child("core-data").projectDir, "src/main/res/values/config.xml")
        )
    }
