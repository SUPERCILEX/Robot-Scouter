package com.supercilex.robotscouter.build

import child
import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.tasks.DeployServer
import com.supercilex.robotscouter.build.tasks.GenerateChangelog
import com.supercilex.robotscouter.build.tasks.RebuildSecrets
import com.supercilex.robotscouter.build.tasks.Setup
import com.supercilex.robotscouter.build.tasks.UpdateTranslations
import com.supercilex.robotscouter.build.tasks.UploadAppToVc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskCollection
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register

internal class RobotScouterBuildPlugin : Plugin<Project> {
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
        project.tasks.register<UpdateTranslations>("updateTranslations") {
            group = "build setup"
            description = "Overwrites existing translations with new ones."

            translationDir.set(project.file("tmp-translations"))
        }

        val presubmit = project.tasks.register("presubmit")
        val generateChangelog = project.child("android-base").tasks
                .register<GenerateChangelog>("generateReleaseChangelog")
        val deployAndroid = project.tasks.register("deployAndroid")
        val uploadAppToVc = project.child("android-base").tasks
                .register<UploadAppToVc>("uploadAppToVersionHistory")
        val deployServer = project.child("functions").tasks.register<DeployServer>("deployServer")

        val ciBuildLifecycle = project.tasks.register("ciBuild")
        val ciBuild = project.tasks.register("buildForCi")

        fun deepFind(matches: String.() -> Boolean) =
                project.allprojects.map { it.tasks.matching { it.name.matches() } }

        fun deepFind(name: String) = deepFind { this == name }

        fun List<TaskCollection<Task>>.mustRunAfter(vararg paths: Any) = onEach {
            it.configureEach { mustRunAfter(paths) }
        }

        presubmit {
            group = "verification"
            description = "Runs a tailored set of checks for the build to pass."

            dependsOn(deepFind("ktlint"))
            dependsOn(":app:android-base:lint" + if (isRelease) "Release" else "Debug")
        }
        ciBuild {
            dependsOn(presubmit)
            dependsOn(":app:server:functions:assemble")
            dependsOn(if (isRelease) {
                listOf(":app:android-base:assembleRelease", ":app:android-base:bundleRelease")
            } else {
                ":app:android-base:assembleDebug"
            })
        }

        ciBuildLifecycle {
            group = "build"
            description = "Runs a specialized build for CI."

            dependsOn(ciBuild)
            if (isRelease) {
                dependsOn(deployAndroid, deployServer, uploadAppToVc)
            }
        }

        run {
            deepFind { startsWith("publish") && endsWith("Bundle") }.mustRunAfter(generateChangelog)
            val publish = deepFind("publish")
            val promote = deepFind {
                startsWith("promote") && endsWith("Artifact")
            }.mustRunAfter(publish)

            deployAndroid {
                group = "publishing"
                description = "Deploys Robot Scouter to the Play Store."

                dependsOn(generateChangelog)
                dependsOn(publish)
                dependsOn(promote)
                dependsOn(deepFind("crashlyticsUploadDeobs"))
            }
        }
        uploadAppToVc {
            dependsOn(ciBuild)
        }

        deployServer {
            group = "publishing"
            description = "Deploys Robot Scouter to the Web and Backend."

            dependsOn("assemble")
        }
    }
}
