package com.supercilex.robotscouter.build

import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.secrets
import com.supercilex.robotscouter.build.tasks.DeployAndroidPrep
import com.supercilex.robotscouter.build.tasks.DeployServer
import com.supercilex.robotscouter.build.tasks.RebuildSecrets
import com.supercilex.robotscouter.build.tasks.Setup
import com.supercilex.robotscouter.build.tasks.UploadAppToVc
import com.supercilex.robotscouter.build.tasks.UploadAppToVcPrep
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class RobotScouterBuildPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project === project.rootProject) { "Cannot apply build plugin to subprojects." }

        project.tasks.register("setup", Setup::class.java).configure {
            group = "build setup"
            description = "Performs one-time setup to prepare Robot Scouter for building."
        }
        project.tasks.register("rebuildSecrets", RebuildSecrets::class.java).configure {
            group = "build setup"
            description = "Repackages a new version of the secrets for CI."
        }

        val deployAndroidPrep =
                project.tasks.register("deployAndroidPrep", DeployAndroidPrep::class.java)
        val deployAndroid = project.tasks.register("deployAndroid")
        val uploadAppToVcPrep =
                project.tasks.register("uploadAppToVcPrep", UploadAppToVcPrep::class.java)
        val uploadAppToVc =
                project.tasks.register("uploadAppToVersionHistory", UploadAppToVc::class.java)
        val deployServer = project.tasks.register("deployServer", DeployServer::class.java)

        val ciBuildLifecycle = project.tasks.register("ciBuild")
        val ciBuildPrep = project.tasks.register("buildForCiPrep")
        val ciBuild = project.tasks.register("buildForCi")

        project.gradle.taskGraph.whenReady {
            val creds = project.secrets.single { it.name.contains("publish") }
            if (!creds.exists()) {
                project.getTasksByName("processReleaseMetadata", true).forEach {
                    it.enabled = false
                }
            }
        }

        project.afterEvaluate {
            ciBuildPrep.configure {
                dependsOn(getTasksByName("clean", true))

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
                        getTasksByName("assembleDebug", true).skip(true)
                    } else {
                        getTasksByName("testReleaseUnitTest", true).skip(true)
                    }
                    getTasksByName("lint", true).skip()
                }
            }
            ciBuild.configure {
                dependsOn(ciBuildPrep)

                fun Collection<Task>.config() = onEach {
                    it.mustRunAfter(ciBuildPrep, deployAndroidPrep)
                }

                fun String.config() = listOf(tasks.getByPath(this)).config()

                if (isRelease) {
                    dependsOn(getTasksByName("build", true).config())
                    dependsOn("app:android-base:bundleRelease".config())
                } else {
                    dependsOn("app:android-base:assembleDebug".config())
                    dependsOn(getTasksByName("check", true).config())
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

                dependsOn(deployAndroidPrep)
                dependsOn(getTasksByName("publish", true))
                dependsOn(getTasksByName("crashlyticsUploadDeobs", true))
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
