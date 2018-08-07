package com.supercilex.robotscouter.build.tasks

import child
import com.google.common.io.Files
import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.shell
import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class UploadAppToVc : DefaultTask() {
    @TaskAction
    fun upload() {
        check(isRelease) { "This action cannot be performed in a dev build." }

        val uploadDir = File(project.rootDir.parentFile, "uploads")

        Grgit.clone {
            it.credentials = Credentials(System.getenv("GIT_TOKEN"))
            it.uri = "https://github.com/SUPERCILEX/app-version-history.git"
            it.dir = uploadDir
        }.use {
            it.repository.jgit.repository.config.apply {
                load()
                setString("user", null, "name", "Alex Saveau")
                setString("user", null, "email", "saveau.alexandre@gmail.com")
            }

            it.add {
                Files.move(
                        File(project.rootDir.parentFile, "app-release.tmp"),
                        File(uploadDir, "Robot-Scouter/app-release.aab")
                )
                File(project.child("android-base").buildDir, "outputs/mapping/release/mapping.txt")
                        .copyTo(File(uploadDir, "Robot-Scouter/mapping.txt"), true)

                it.patterns = setOf("Robot-Scouter")
                it.update = true
            }
            it.commit {
                val dump =
                        shell("/usr/local/android-sdk/build-tools/\${BUILD_TOOLS_VERSION}/aapt" +
                                      " dump badging app-base.tmp") {
                            directory(project.rootDir.parentFile)
                        }()
                val versionCode = dump.substringAfter("versionCode='").substringBefore("'")

                it.message = """
                    |$versionCode

                    |https://github.com/SUPERCILEX/Robot-Scouter/compare/${System.getenv("TRAVIS_COMMIT_RANGE")}

                    |Full dump:
                    |$dump
                """.trimMargin()
            }
            it.push {
                it.refsOrSpecs = listOf("master")
            }
        }
    }
}
