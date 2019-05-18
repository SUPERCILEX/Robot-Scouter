package com.supercilex.robotscouter.build.tasks

import child
import com.google.common.io.Files
import com.supercilex.robotscouter.build.internal.orNull
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

@CacheableTask
open class UpdateTranslations : DefaultTask() {
    @get:Input
    @set:Option(option = "locale", description = "Set the language to update.")
    var language: String? = null
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    val translationFolder = project.file("tmp-translations")

    @get:OutputFiles protected val playStoreFiles by lazy {
        val listings = project.child("android-base").file("src/main/play/listings/$language")
        listOf(File(listings, "short-description.txt"), File(listings, "full-description.txt"))
    }
    @get:OutputFiles protected val stringFiles by lazy {
        project.subprojects.mapNotNull {
            it.file("src/main/res/values/strings.xml").orNull()
        }.map {
            File(it.parentFile.parentFile, "values-$language/strings.xml")
        }
    }

    @TaskAction
    fun update() {
        overwrite(playStoreFiles) { nameWithoutExtension.contains(it) }
        overwrite(stringFiles) { invariantSeparatorsPath.split("/").contains(it) }

        if (translationFolder.listFiles().isEmpty()) translationFolder.delete()
    }

    private fun overwrite(files: List<File>, predicate: File.(String) -> Boolean) {
        val translations = translationFolder.listFiles()
        for (file in files) {
            val from = translations.singleOrNull {
                // Transifex uses a 'foo_bar_resource-name_en.extension' format
                val normalized = it.nameWithoutExtension.split("_").run { get(lastIndex - 1) }
                file.predicate(normalized)
            } ?: continue
            Files.move(from, file)
        }
    }
}
