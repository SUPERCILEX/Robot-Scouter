package com.supercilex.robotscouter.build.tasks

import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import javax.inject.Inject

@CacheableTask
open class GenerateChangelog : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    protected val commitRange = project.rootProject.file("CIRCLE_COMPARE_URL.txt")
    @get:OutputFiles
    protected val files: List<File>

    init {
        val base = project.file("src/main/play/release-notes/en-US")
        files = listOf(File(base, "internal.txt"), File(base, "alpha.txt"))
    }

    @TaskAction
    fun generateChangelog() {
        project.serviceOf<WorkerExecutor>().submit(Generator::class) {
            params(Generator.Params(commitRange, files))
        }
    }

    private class Generator @Inject constructor(private val p: Params) : Runnable {
        override fun run() {
            val base = p.commitRange.readText().substringBefore(".")

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

                for (output in p.files) output.writeText(changelog)
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

        data class Params(val commitRange: File, val files: List<File>) : Serializable
    }
}
