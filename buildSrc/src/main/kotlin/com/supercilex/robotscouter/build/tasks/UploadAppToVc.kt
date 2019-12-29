package com.supercilex.robotscouter.build.tasks

import com.google.common.io.Files
import com.supercilex.robotscouter.build.internal.isRelease
import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

internal abstract class UploadAppToVc : DefaultTask() {
    @TaskAction
    fun upload() {
        check(isRelease) { "This action cannot be performed in a dev build." }
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Uploader::class) {
            outerDir.set(project.rootDir.parentFile)
            buildDir.set(project.buildDir)
        }
    }

    abstract class Uploader @Inject constructor(
            private val execOps: ExecOperations
    ) : WorkAction<Uploader.Params> {
        override fun execute() {
            val uploadDir = parameters.outerDir.dir("uploads").get()

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

        private fun Grgit.stage(uploadDir: Directory) = add {
            Files.move(
                    parameters.buildDir
                            .file("outputs/bundle/release/android-base-release.aab").get().asFile,
                    uploadDir.file("Robot-Scouter/app-release.aab").asFile
            )
            parameters.buildDir.file("outputs/mapping/release/mapping.txt").get().asFile
                    .copyTo(uploadDir.file("Robot-Scouter/mapping.txt").asFile, true)

            patterns = setOf("Robot-Scouter")
            update = true
        }

        private fun Grgit.commitChanges() = commit {
            val buildTools = File("${System.getenv("ANDROID_HOME")}/build-tools")
                    .listFiles()
                    .orEmpty()
                    .sortedDescending()
                    .first()

            val output = ByteArrayOutputStream()
            execOps.exec {
                workingDir = parameters.buildDir.dir("outputs/apk/release").get().asFile
                commandLine("sh", "-c", "${buildTools.absolutePath}/aapt" +
                        " dump badging android-base-release.apk")
                standardOutput = output
            }
            val dump = output.toString()
            val versionCode = dump.substringAfter("versionCode='").substringBefore("'")

            message = """
                |$versionCode

                |${File("CIRCLE_COMPARE_URL.txt").readText()}

                |Full dump:
                |$dump
            """.trimMargin()
        }

        private fun Grgit.pushChanges() = push {
            refsOrSpecs = listOf("master")
        }

        interface Params : WorkParameters {
            val outerDir: DirectoryProperty
            val buildDir: DirectoryProperty
        }
    }
}
