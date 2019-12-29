package com.supercilex.robotscouter.build.tasks

import child
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import com.supercilex.robotscouter.build.internal.isRelease
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
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
import java.io.ByteArrayOutputStream
import javax.inject.Inject

internal abstract class DeployServer : DefaultTask() {
    @get:Optional
    @get:Input
    @set:Option(option = "only", description = "See firebase help documentation")
    var only: String? = null
    @get:Input
    @set:Option(option = "update-templates", description = "Trigger default templates update")
    var updateTemplates: Boolean = false

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val transpiledJs = project.layout.buildDirectory
            .file("classes/kotlin/main/functions.js")
    @get:OutputFile
    protected val targetJs = project.layout.projectDirectory.file("upload/index.js")

    init {
        inputs.file(project.file("upload/package-lock.json"))
    }

    @TaskAction
    fun deploy() {
        project.configurations.getByName("compileClasspath").filter {
            it.absolutePath.contains(project.rootDir.absolutePath)
        }.forEach { file ->
            project.copy {
                includeEmptyDirs = false

                from(project.zipTree(file.absolutePath))
                into("upload/common")
                include { it.name == "common.js" }
                rename("common.js", "index.js")
            }
        }
        transpiledJs.get().asFile.copyTo(targetJs.asFile, true)

        project.serviceOf<WorkerExecutor>().noIsolation().submit(Deployer::class) {
            functionsDir.set(project.layout.projectDirectory.dir("upload"))
            serverDir.set(project.rootProject.child("server").projectDir)
            onlyFlag.set(only)
            updateTemplatesFlag.set(updateTemplates)
        }
    }

    abstract class Deployer @Inject constructor(
            private val execOps: ExecOperations
    ) : WorkAction<Deployer.Params> {
        override fun execute() {
            installIfNeeded("npm -v", "npm", "6.13.4")
            installIfNeeded("firebase -V", "firebase-tools", "7.11.0")
            execOps.exec {
                workingDir = parameters.functionsDir.get().asFile
                commandLine("sh", "-c", "npm ci")
            }

            deployFirebase()

            if (isRelease || parameters.updateTemplatesFlag.get()) {
                Thread.sleep(30_000) // Wait to ensure function has redeployed
                triggerDefaultTemplateUpdate()
            }
        }

        private fun deployFirebase() {
            var command = "firebase deploy --non-interactive"
            parameters.onlyFlag.orNull?.let { command += " --only $it" }
            execOps.exec {
                workingDir = parameters.serverDir.get().asFile
                commandLine("sh", "-c", command)

                // The admin SDK will try to read the gpp creds file otherwise
                environment.remove("GOOGLE_APPLICATION_CREDENTIALS")
            }
        }

        private fun triggerDefaultTemplateUpdate() {
            val updateTemplates = ProjectTopicName.of(
                    "robot-scouter-app", "update-default-templates")
            val messageId = Publisher.newBuilder(updateTemplates).build()
                    .publish(PubsubMessage.newBuilder()
                                     .setData(ByteString.copyFromUtf8("{}"))
                                     .build())
                    .get()
            println("Triggered default template updates with message id: $messageId")
        }

        private fun installIfNeeded(command: String, name: String, version: String) {
            val existing = try {
                val output = ByteArrayOutputStream()
                execOps.exec {
                    commandLine("sh", "-c", command)
                    standardOutput = output
                }
                output.toString().trim()
            } catch (e: Exception) {
                null
            }

            if (existing != version) {
                execOps.exec {
                    commandLine("sh", "-c", "npm install -gq $name@$version")
                }
            }
        }

        interface Params : WorkParameters {
            val functionsDir: DirectoryProperty
            val serverDir: DirectoryProperty

            val onlyFlag: Property<String>
            val updateTemplatesFlag: Property<Boolean>
        }
    }
}
