package com.supercilex.robotscouter.build.tasks

import com.supercilex.robotscouter.build.internal.secrets
import com.supercilex.robotscouter.build.internal.shell
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

@CacheableTask
open class RebuildSecrets : DefaultTask() {
    @Option(description = "Password for encryption")
    @get:Input
    var password: String? = null
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    protected val files = secrets.ci
    @get:OutputFile protected val output = project.file("secrets.tar.enc")

    @TaskAction
    fun rebuild() {
        val rawSecrets = output.nameWithoutExtension

        val tarTargets = files.map {
            it.relativeTo(project.projectDir)
        }.joinToString(" ").replace("\\", "/")
        shell("tar -cvf $rawSecrets $tarTargets")
        shell("openssl aes-256-cbc -md sha256 -e -p -pass 'pass:$password'" +
                      " -in $rawSecrets -out ${output.name}")
        Grgit.open().use { it.add { patterns = setOf(output.name) } }

        project.delete(rawSecrets)
    }
}
