package com.supercilex.robotscouter.build

import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.secrets
import com.supercilex.robotscouter.build.tasks.DeployServer
import com.supercilex.robotscouter.build.tasks.GenerateChangelog
import com.supercilex.robotscouter.build.tasks.RebuildSecrets
import com.supercilex.robotscouter.build.tasks.Setup
import com.supercilex.robotscouter.build.tasks.UploadAppToVc
import com.supercilex.robotscouter.build.tasks.UploadAppToVcPrep
import deepFind
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.register

class RobotScouterBuildPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project === project.rootProject) { "Cannot apply build plugin to subprojects." }

        project.tasks.register<Setup>("setup") {
            group = "build setup"
            description = "Performs one-time setup to prepare Robot Scouter for building."
        }
        project.tasks.register<RebuildSecrets>("rebuildSecrets") {
            group = "build setup"
            description = "Repackages a new version of the secrets for CI."
        }

        val generateChangelog =
                project.tasks.register<GenerateChangelog>("generateReleaseChangelog")
        val deployAndroid = project.tasks.register("deployAndroid")
        val uploadAppToVcPrep = project.tasks.register<UploadAppToVcPrep>("uploadAppToVcPrep")
        val uploadAppToVc =
                project.tasks.register<UploadAppToVc>("uploadAppToVersionHistory")
        val deployServer = project.tasks.register<DeployServer>("deployServer")

        val ciBuildLifecycle = project.tasks.register("ciBuild")
        val ciBuildPrep = project.tasks.register("buildForCiPrep")
        val ciBuild = project.tasks.register("buildForCi")

        project.gradle.taskGraph.whenReady {
            val creds = project.secrets.single { it.name.contains("publish") }
            if (!creds.exists()) {
                allTasks.filter { it.name == "processReleaseMetadata" }.forEach {
                    it.enabled = false
                }
            }
        }

        project.afterEvaluate {
            fun <T : Collection<Task>> T.mustRunAfter(vararg paths: Any) =
                    onEach { it.mustRunAfter(paths) }

            ciBuildPrep.configure {
                dependsOn(deepFind("clean"))

                gradle.taskGraph.whenReady {
                    if (!hasTask(this@configure)) return@whenReady

                    fun Collection<Task>.skip(recursive: Boolean = false): Unit = forEach {
                        it.enabled = false
                        if (recursive) {
                            if (hasTask(it)) {
                                getDependencies(it).filter { it.enabled }.skip(recursive)
                            }
                        }
                    }

                    if (isRelease) {
                        deepFind("assembleDebug").skip(true)
                    } else {
                        deepFind("testReleaseUnitTest").skip(true)
                    }
                    deepFind("lint").skip()
                }
            }
            ciBuild.configure {
                dependsOn(ciBuildPrep)

                fun Collection<Task>.config() = apply {
                    mustRunAfter(ciBuildPrep)
                }

                fun String.config() = deepFind { it.path == this }.config()

                if (isRelease) {
                    dependsOn(deepFind("build").config())
                    dependsOn(":app:android-base:bundleRelease".config())
                } else {
                    dependsOn(":app:android-base:assembleDebug".config())
                    dependsOn(deepFind("check").config())
                }
            }

            ciBuildLifecycle.configure {
                group = "build"
                description = "Runs a specialized build for CI."

                dependsOn(ciBuild)
                if (isRelease) {
                    dependsOn(deployAndroid, deployServer, uploadAppToVc)
                }
            }

            deployAndroid.configure {
                group = "publishing"
                description = "Deploys Robot Scouter to the Play Store."

                dependsOn(generateChangelog)
                dependsOn(deepFind("publish").mustRunAfter(generateChangelog))
                dependsOn(deepFind("crashlyticsUploadDeobs"))
            }
            uploadAppToVcPrep.configure {
                dependsOn(ciBuild)
            }
            uploadAppToVc.configure {
                dependsOn(uploadAppToVcPrep)
            }

            deployServer.configure {
                group = "publishing"
                description = "Deploys Robot Scouter to the Web and Backend."

                dependsOn("app:server:functions:assemble")
            }
        }
    }
}
