package com.supercilex.robotscouter.build.tasks

import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

internal abstract class GenerateChangelog : DefaultTask() {
    @get:OutputFiles
    protected val files by lazy {
        val base = project.layout.projectDirectory.dir("src/main/play/release-notes/en-US")
        listOf(base.file("internal.txt"), base.file("alpha.txt"))
    }

    @TaskAction
    fun generateChangelog() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Generator::class) {
            projectDir.set(project.projectDir)
            changelogFiles.set(files)
        }
    }

    abstract class Generator : WorkAction<Generator.Params> {
        override fun execute() {
            Grgit.open {
                currentDir = parameters.projectDir.get().asFile
            }.use {
                val recentCommits = it.log {
                    maxCommits = 10
                }

                val changelog = recentCommits
                        .map { "- ${it.shortMessage}" }
                        .generate()

                for (output in parameters.changelogFiles.get()) {
                    output.asFile.writeText(changelog)
                }
            }
        }

        private tailrec fun List<String>.generate(): String {
            val candidate = """
                |Recent changes:
                |${joinToString("\n")}

                |Report bugs and view the full changelog here:
                |https://github.com/SUPERCILEX/Robot-Scouter/commits/master
            """.trimMargin()

            // The Play Store allows a max of 500 chars
            return if (candidate.length > 500) subList(1, size).generate() else candidate
        }

        interface Params : WorkParameters {
            val projectDir: DirectoryProperty
            val changelogFiles: ListProperty<RegularFile>
        }
    }
}
