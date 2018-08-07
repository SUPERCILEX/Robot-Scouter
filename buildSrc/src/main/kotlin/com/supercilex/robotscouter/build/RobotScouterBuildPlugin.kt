package com.supercilex.robotscouter.build

import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.secrets
import com.supercilex.robotscouter.build.tasks.CiPrepForAndroidDeployment
import com.supercilex.robotscouter.build.tasks.DeployServer
import com.supercilex.robotscouter.build.tasks.RebuildSecrets
import com.supercilex.robotscouter.build.tasks.Setup
import com.supercilex.robotscouter.build.tasks.UploadAppToVc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.GradleBuild

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

        val deployAndroid = project.tasks.register("deployAndroid")
        val deployServer = project.tasks.register("deployServer", DeployServer::class.java)
        val uploadAppToVc =
                project.tasks.register("uploadAppToVersionHistory", UploadAppToVc::class.java)

        val ciBuildLifecycle = project.tasks.register("ciBuild")
        val ciBuildPrep = project.tasks.register("buildForCiPrep")
        val ciBuild = project.tasks.register("buildForCi")
        val ciBuildPhase2 = project.tasks.register("buildForCiPhase2", GradleBuild::class.java)
        val ciPrepForAndroidDeployment = project.tasks.register(
                "ciPrepForAndroidDeployment", CiPrepForAndroidDeployment::class.java)

        project.gradle.taskGraph.whenReady {
            val creds = project.secrets.single { it.name.contains("publish") }
            if (!creds.exists()) {
                project.getTasksByName("processReleaseMetadata", true).forEach {
                    it.enabled = false
                }
            }
        }

        project.afterEvaluate {
            fun String.mustRunAfter(vararg paths: Any) = tasks.getByPath(this).mustRunAfter(paths)

            fun <T : Collection<Task>> T.mustRunAfter(vararg paths: Any) =
                    onEach { it.mustRunAfter(paths) }

            ciBuildPrep.configure {
                dependsOn(getTasksByName("clean", true))

                gradle.taskGraph.whenReady {
                    fun Collection<Task>.skip(recursive: Boolean = false): Unit = forEach {
                        it.enabled = false
                        if (recursive) {
                            val graph = gradle.taskGraph
                            if (graph.hasTask(it)) {
                                graph.getDependencies(it).filter { it.enabled }.skip(recursive)
                            }
                        }
                    }

                    if (isRelease) {
                        getTasksByName("assembleDebug", true).skip(true)
                    } else {
                        getTasksByName("processReleaseMetadata", true).skip()
                        getTasksByName("compileReleaseKotlin", true).skip()
                    }
                    getTasksByName("lint", true).skip()
                }
            }
            ciBuild.configure {
                dependsOn(ciBuildPrep)

                if (isRelease) {
                    dependsOn(getTasksByName("build", true).mustRunAfter(ciBuildPrep))
                    dependsOn("app:android-base:bundleRelease".mustRunAfter(ciBuildPrep))
                } else {
                    dependsOn("app:android-base:assembleDebug".mustRunAfter(ciBuildPrep))
                    dependsOn(getTasksByName("check", true).mustRunAfter(ciBuildPrep))
                }
            }
            ciBuildPhase2.configure {
                // This task requires a full Gradle build because app packaging tasks must be re-run
                // to apply the new signing keys.

                startParameter = project.gradle.startParameter.newInstance()
                tasks = listOf(deployAndroid.name)

                mustRunAfter(ciPrepForAndroidDeployment)
            }

            ciBuildLifecycle.configure {
                group = "build"
                description = "Runs a specialized build for CI."

                dependsOn(ciBuild)
                if (isRelease) {
                    dependsOn(ciPrepForAndroidDeployment, ciBuildPhase2)
                    dependsOn(deployServer, uploadAppToVc)
                }
            }
            ciPrepForAndroidDeployment.configure {
                dependsOn(ciBuild)
            }
            deployAndroid.configure {
                group = "publishing"
                description = "Deploys Robot Scouter to the Play Store."

                dependsOn(getTasksByName("publish", true))
                dependsOn(getTasksByName("crashlyticsUploadDeobs", true))
            }
            deployServer.configure {
                group = "publishing"
                description = "Deploys Robot Scouter to the Web and Backend."

                dependsOn("app:server:functions:assemble")
            }
            uploadAppToVc.configure {
                dependsOn(ciPrepForAndroidDeployment)
            }
        }
    }
}
