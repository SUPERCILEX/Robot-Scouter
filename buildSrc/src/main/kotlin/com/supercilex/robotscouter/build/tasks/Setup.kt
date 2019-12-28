package com.supercilex.robotscouter.build.tasks

import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.orNull
import com.supercilex.robotscouter.build.internal.redactLogs
import com.supercilex.robotscouter.build.internal.secrets
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

internal abstract class Setup : DefaultTask() {
    @TaskAction
    fun setup() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(SecretsExtractor::class) {
            projectDir.set(project.projectDir)
            secretFiles.set(secrets.full)
        }
    }

    abstract class SecretsExtractor @Inject constructor(
            private val execOps: ExecOperations
    ) : WorkAction<SecretsExtractor.Params> {
        override fun execute() {
            var success = extractRawSecrets()
            if (!success) success = extractEncryptedSecrets()
            if (!success) success = extractDummies()

            check(success) { "Project file extraction failed." }
        }

        private fun extractRawSecrets(): Boolean {
            val secrets = parameters.projectDir
                    .file("secrets.tar").get().asFile.orNull() ?: return false
            execOps.exec {
                commandLine("sh", "-c", "tar -xvf ${secrets.name}")
                redactLogs()
            }
            return true
        }

        private fun extractEncryptedSecrets(): Boolean {
            val secrets = parameters.projectDir
                    .file("secrets.tar.enc").get().asFile.orNull() ?: return false
            val password = System.getenv("SECRETS_PASS") ?: return false

            execOps.exec {
                commandLine("sh", "-c", "openssl aes-256-cbc -md sha256 -d -k '$password' " +
                        "-in ${secrets.name} -out ${secrets.nameWithoutExtension}")
                redactLogs()
            }

            return extractRawSecrets()
        }

        private fun extractDummies(): Boolean {
            check(!isRelease) { "Cannot use dummies for release builds." }

            val dummies = parameters.projectDir.dir("ci-dummies").get().asFileTree
            if (dummies.isEmpty) return false

            for (dummy in dummies) {
                val dest = parameters.secretFiles.get()
                        .map { it.asFile }
                        .first { it.name == dummy.name }
                if (!dest.exists()) dummy.copyTo(dest)
            }

            return true
        }

        interface Params : WorkParameters {
            val projectDir: DirectoryProperty
            val secretFiles: ListProperty<RegularFile>
        }
    }
}
