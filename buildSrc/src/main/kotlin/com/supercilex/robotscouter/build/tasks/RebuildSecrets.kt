package com.supercilex.robotscouter.build.tasks

import com.supercilex.robotscouter.build.internal.secrets
import com.supercilex.robotscouter.build.internal.shell
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
open class RebuildSecrets : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val files = secrets
    @get:OutputFile protected val output = project.file("secrets.tar.enc")

    @TaskAction
    fun rebuild() {
        val rawSecrets = output.nameWithoutExtension

        val tarTargets = files.map { it.relativeTo(project.projectDir) }.joinToString(" ")
        shell("tar -cvf $rawSecrets $tarTargets")
        shell("travis encrypt-file -f $rawSecrets")
        Grgit.open().use { it.add { patterns = setOf(output.name) } }

        project.delete(rawSecrets)
    }
}
