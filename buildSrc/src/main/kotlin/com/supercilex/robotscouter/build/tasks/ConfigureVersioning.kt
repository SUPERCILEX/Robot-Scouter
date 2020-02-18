package com.supercilex.robotscouter.build.tasks

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class ConfigureVersioning @Inject constructor(
        private val variant: ApplicationVariant
) : DefaultTask() {
    @TaskAction
    fun configure() {
        val execOps = project.serviceOf<ExecOperations>()

        val versionName = run {
            val output = ByteArrayOutputStream()
            execOps.exec {
                commandLine("git", "describe", "--tags", "--dirty", "--always")
                standardOutput = output
            }
            output.toString().trim()
        }

        variant.outputs.filterIsInstance<ApkVariantOutput>().forEach { output ->
            output.versionNameOverride = versionName
            output.outputFileName = "robot-scouter-$versionName.apk"
        }
    }
}
