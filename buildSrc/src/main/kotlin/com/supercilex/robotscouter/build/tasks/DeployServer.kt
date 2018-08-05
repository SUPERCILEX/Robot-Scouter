package com.supercilex.robotscouter.build.tasks

import child
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.shell
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class DeployServer : DefaultTask() {
    @Option(description = "See firebase help documentation")
    var only: String? = null

    @get:InputFile protected val transpiledJs: File
    @get:OutputFile protected val targetJs: File

    init {
        val functions = project.child("functions")
        transpiledJs = File(functions.buildDir, "classes/kotlin/main/firebase.js")
        targetJs = File(functions.projectDir, "index.js")
    }

    @TaskAction
    fun deploy() {
        transpiledJs.copyTo(targetJs, true)

        var command = "firebase deploy --non-interactive"
        only?.let { command += " --only $only" }
        shell(command) { directory(project.child("server").projectDir) }

        if (isRelease) {
            val updateTemplates = ProjectTopicName.of(
                    "robot-scouter-app", "update-default-templates")
            val messageId = Publisher.newBuilder(updateTemplates).build()
                    .publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("{}")).build())
                    .get()
            println("Triggered default template updates with message id: $messageId")
        }
    }
}
