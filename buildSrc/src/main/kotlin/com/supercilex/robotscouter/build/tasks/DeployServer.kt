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
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class DeployServer : DefaultTask() {
    @Option(description = "See firebase help documentation")
    @get:Optional
    @get:Input
    var only: String? = null
    @Option(option = "update-templates", description = "Trigger default templates update")
    @get:Input
    var updateTemplates: Boolean = false

    @get:InputFile protected val transpiledJs: File
    @get:OutputFile protected val targetJs: File

    private val functionsProject = project.child("functions")

    init {
        transpiledJs = File(functionsProject.buildDir, "classes/kotlin/main/functions.js")
        targetJs = File(functionsProject.projectDir, "index.js")
        inputs.file(File(functionsProject.projectDir, "package-lock.json"))
    }

    @TaskAction
    fun deploy() {
        for (file in functionsProject.configurations.getByName("compile")) {
            functionsProject.copy {
                includeEmptyDirs = false

                from(functionsProject.zipTree(file.absolutePath))
                into("common")
                include { it.name == "common.js" }
                rename("common.js", "index.js")
            }
        }
        shell("npm ci") { directory(functionsProject.projectDir) }

        transpiledJs.copyTo(targetJs, true)

        var command = "firebase deploy --non-interactive"
        only?.let { command += " --only $only" }
        shell(command) { directory(project.child("server").projectDir) }

        if (isRelease || updateTemplates) {
            Thread.sleep(30_000) // Wait to ensure function has redeployed

            val updateTemplates = ProjectTopicName.of(
                    "robot-scouter-app", "update-default-templates")
            val messageId = Publisher.newBuilder(updateTemplates).build()
                    .publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("{}")).build())
                    .get()
            println("Triggered default template updates with message id: $messageId")
        }
    }
}
