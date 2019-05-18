package com.supercilex.robotscouter.build.tasks

import com.google.common.io.Files
import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.shell
import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

open class UploadAppToVc : DefaultTask() {
    @TaskAction
    fun upload() {
        check(isRelease) { "This action cannot be performed in a dev build." }
        project.serviceOf<WorkerExecutor>().submit(Uploader::class) {
            params(Uploader.Params(project.rootDir.parentFile, project.buildDir))
        }
    }

    private class Uploader @Inject constructor(private val p: Params) : Runnable {
        override fun run() {
            val uploadDir = File(p.outerDir, "uploads")

            Grgit.clone {
                credentials = Credentials(System.getenv("GIT_TOKEN"))
                uri = "https://github.com/SUPERCILEX/app-version-history.git"
                dir = uploadDir
            }.use {
                it.setup()
                it.stage(uploadDir)
                it.commitChanges()
                it.pushChanges()
            }
        }

        private fun Grgit.setup() {
            repository.jgit.repository.config.apply {
                load()
                setString("user", null, "name", "Alex Saveau")
                setString("user", null, "email", "saveau.alexandre@gmail.com")
            }
        }

        private fun Grgit.stage(uploadDir: File) = add {
            Files.move(
                    File(p.outerDir, "app-release.tmp"),
                    File(uploadDir, "Robot-Scouter/app-release.aab")
            )
            File(
                    p.buildDir,
                    "outputs/mapping/release/mapping.txt"
            ).copyTo(File(uploadDir, "Robot-Scouter/mapping.txt"), true)

            patterns = setOf("Robot-Scouter")
            update = true
        }

        private fun Grgit.commitChanges() = commit {
            val buildToolsVersion = File("/usr/local/android-sdk/build-tools")
                    .listFiles()
                    .sortedDescending()
                    .first().name

            val dump =
                    shell("/usr/local/android-sdk/build-tools/$buildToolsVersion/aapt" +
                                  " dump badging app-base.tmp") {
                        directory(p.outerDir)
                    }()
            val versionCode = dump.substringAfter("versionCode='").substringBefore("'")

            message = """
                |$versionCode

                |https://github.com/SUPERCILEX/Robot-Scouter/compare/${System.getenv("TRAVIS_COMMIT_RANGE")}

                |Full dump:
                |$dump
            """.trimMargin()
        }

        private fun Grgit.pushChanges() = push {
            refsOrSpecs = listOf("master")
        }

        data class Params(val outerDir: File, val buildDir: File) : Serializable
    }
}
