package com.supercilex.robotscouter.build.tasks

import child
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
open class GenerateChangelog : DefaultTask() {
    @get:Input
    protected val commitRange by lazy {
        checkNotNull(System.getenv("TRAVIS_COMMIT_RANGE")) {
            "This action cannot be performed in a dev build."
        }
    }
    @get:OutputFiles
    protected val files: List<File>

    init {
        val base = project.child("android-base").file("src/main/play/release-notes/en-US")
        files = listOf(File(base, "internal"), File(base, "alpha"))
    }

    @TaskAction
    fun generateChangelog() {
        val (old, head) = commitRange.run { substringBefore(".") to substringAfterLast(".") }

        Grgit.open().use {
            val changelog = it.log {
                it.range(old, head)
            }.reversed().map {
                "- ${it.shortMessage}"
            }.generate()

            for (output in files) {
                output.writeText(changelog)
            }
        }
    }

    private fun List<String>.generate(): String {
        val candidate = """
            |${joinToString("\n")}

            |Report bugs and view the full changelog here: https://github.com/SUPERCILEX/Robot-Scouter/commits/master
        """.trimMargin()

        // The Play Store allows a max of 500 chars
        return if (candidate.length > 500) subList(1, size).generate() else candidate
    }
}
