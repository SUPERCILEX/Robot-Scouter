package com.supercilex.robotscouter.build.tasks

import com.supercilex.robotscouter.build.internal.secrets
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
internal abstract class RebuildSecrets : DefaultTask() {
    @get:Input
    @set:Option(option = "password", description = "Password for encryption")
    var password: String? = null
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val files = secrets.ci

    @get:OutputFile
    protected val output = project.file("secrets.tar.enc")

    @TaskAction
    fun rebuild() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(SecretsCreator::class) {
            projectDir.set(project.projectDir)
            secretFiles.set(files)
            secretsFile.set(output)
            secret.set(password)
        }
    }

    abstract class SecretsCreator @Inject constructor(
            private val fileOps: FileSystemOperations,
            private val execOps: ExecOperations
    ) : WorkAction<SecretsCreator.Params> {
        override fun execute() {
            val output = parameters.secretsFile.get().asFile
            val rawSecrets = output.nameWithoutExtension

            val tarTargets = parameters.secretFiles.get().map {
                it.asFile.relativeTo(parameters.projectDir.get().asFile)
            }.joinToString(" ").replace("\\", "/")
            execOps.exec {
                commandLine("sh", "-c", "tar -cvf $rawSecrets $tarTargets")
            }
            execOps.exec {
                val password = parameters.secret.get()
                commandLine("sh", "-c", "openssl aes-256-cbc -md sha256 -e -p " +
                        "-pass 'pass:$password' " +
                        "-in $rawSecrets -out ${output.name}")
            }
            Grgit.open {
                dir = parameters.projectDir.get().asFile
            }.use {
                it.add { patterns = setOf(output.name) }
            }

            fileOps.delete { delete(rawSecrets) }
        }

        interface Params : WorkParameters {
            val projectDir: DirectoryProperty
            val secretFiles: ListProperty<RegularFile>
            val secretsFile: RegularFileProperty
            val secret: Property<String>
        }
    }
}
