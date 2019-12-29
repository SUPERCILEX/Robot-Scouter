package com.supercilex.robotscouter.build.tasks

import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

@CacheableTask
internal abstract class GenerateChangelog : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val baseCommit = project.rootProject.layout.projectDirectory
            .file("BASE_COMPARE_COMMIT.txt")
    @get:OutputFiles
    protected val files by lazy {
        val base = project.layout.projectDirectory.dir("src/main/play/release-notes/en-US")
        listOf(base.file("internal.txt"), base.file("alpha.txt"))
    }

    @TaskAction
    fun generateChangelog() {
        project.serviceOf<WorkerExecutor>().noIsolation().submit(Generator::class) {
            baseCommitFile.set(baseCommit)
            changelogFiles.set(files)
        }
    }

    abstract class Generator : WorkAction<Generator.Params> {
        override fun execute() {
            val base = parameters.baseCommitFile.get().asFile.readText().take(20)

            Grgit.open().use {
                val recentCommits = it.log {
                    maxCommits = 10
                }
                val baseIndex = recentCommits.indexOfFirst { it.id.startsWith(base) }

                val changelog = recentCommits
                        .take(baseIndex.coerceAtLeast(1))
                        .reversed()
                        .map { "- ${it.shortMessage}" }
                        .generate()

                for (output in parameters.changelogFiles.get()) {
                    output.asFile.writeText(changelog)
                }
            }
        }

        private tailrec fun List<String>.generate(): String {
            val candidate = """
                |${joinToString("\n")}

                |Report bugs and view the full changelog here:
                |https://github.com/SUPERCILEX/Robot-Scouter/commits/master
            """.trimMargin()

            // The Play Store allows a max of 500 chars
            return if (candidate.length > 500) subList(1, size).generate() else candidate
        }

        interface Params : WorkParameters {
            val baseCommitFile: RegularFileProperty
            val changelogFiles: ListProperty<RegularFile>
        }
    }
}
