package com.supercilex.robotscouter.build.tasks

import child
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.shell
import org.gradle.api.DefaultTask
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
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

open class DeployServer : DefaultTask() {
    @Option(description = "See firebase help documentation")
    @get:Optional
    @get:Input
    var only: String? = null
    @Option(option = "update-templates", description = "Trigger default templates update")
    @get:Input
    var updateTemplates: Boolean = false

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val transpiledJs = File(project.buildDir, "classes/kotlin/main/functions.js")
    @get:OutputFile
    protected val targetJs = File(project.projectDir, "index.js")

    init {
        inputs.file(File(project.projectDir, "package-lock.json"))
    }

    @TaskAction
    fun deploy() {
        for (file in project.configurations.getByName("compile")) {
            project.copy {
                includeEmptyDirs = false

                from(project.zipTree(file.absolutePath))
                into("common")
                include { it.name == "common.js" }
                rename("common.js", "index.js")
            }
        }
        transpiledJs.copyTo(targetJs, true)

        project.serviceOf<WorkerExecutor>().submit(Deployer::class) {
            params(Deployer.Params(
                    project.projectDir,
                    project.rootProject.child("server").projectDir,
                    only,
                    updateTemplates
            ))
        }
    }

    private class Deployer @Inject constructor(private val p: Params) : Runnable {
        override fun run() {
            installIfNeeded("npm -v", "npm", "6.9.2")
            installIfNeeded("firebase -V", "firebase-tools", "7.0.2")
            shell("npm ci") { directory(p.functionsDir) }

            var command = "firebase deploy --non-interactive"
            p.only?.let { command += " --only ${p.only}" }
            shell(command) {
                directory(p.serverDir)
                // The admin SDK will try to read the PubSub cred file, but it's in the wrong
                // directory
                environment() -= "GOOGLE_APPLICATION_CREDENTIALS"
            }

            if (isRelease || p.updateTemplates) {
                Thread.sleep(30_000) // Wait to ensure function has redeployed

                val updateTemplates = ProjectTopicName.of(
                        "robot-scouter-app", "update-default-templates")
                val messageId = Publisher.newBuilder(updateTemplates).build()
                        .publish(PubsubMessage.newBuilder()
                                         .setData(ByteString.copyFromUtf8("{}"))
                                         .build())
                        .get()
                println("Triggered default template updates with message id: $messageId")
            }
        }

        private fun installIfNeeded(command: String, name: String, version: String) {
            val existing = try {
                shell(command)()
            } catch (e: Exception) {
                null
            }

            if (existing != version) shell("npm install -gq $name@$version")
        }

        data class Params(
                val functionsDir: File,
                val serverDir: File,

                val only: String?,
                val updateTemplates: Boolean
        ) : Serializable
    }
}
