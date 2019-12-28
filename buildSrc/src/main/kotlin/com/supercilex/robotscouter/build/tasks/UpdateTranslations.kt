package com.supercilex.robotscouter.build.tasks

import child
import com.google.common.io.Files
import com.supercilex.robotscouter.build.internal.orNull
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.submit
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class UpdateTranslations : DefaultTask() {
    @get:Input
    @set:Option(option = "locale", description = "Set the language to update.")
    var language: String? = null
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val translationDir: DirectoryProperty

    @get:OutputFiles
    protected val playListingFiles by lazy {
        val listings = project.child("android-base").layout.projectDirectory
                .dir("src/main/play/listings/$language")
        listOf(listings.file("short-description.txt"), listings.file("full-description.txt"))
    }
    @get:OutputFiles
    protected val resourceFiles by lazy {
        project.subprojects.mapNotNull {
            it.file("src/main/res/values/strings.xml").orNull()
        }.map {
            project.layout.projectDirectory.file("src/main/res/values-$language/strings.xml")
        }
    }

    @TaskAction
    fun update() {
        val inputTranslations = translationDir
        val outputPlayMetadata = playListingFiles
        val outputResources = resourceFiles
        project.serviceOf<WorkerExecutor>().noIsolation().submit(TranslationUpdater::class) {
            translationDir.set(inputTranslations)
            playListingFiles.set(outputPlayMetadata)
            resourceFiles.set(outputResources)
        }
    }

    abstract class TranslationUpdater @Inject constructor(
            private val fileOps: FileSystemOperations
    ) : WorkAction<TranslationUpdater.Params> {
        override fun execute() {
            overwrite(parameters.playListingFiles) { nameWithoutExtension.contains(it) }
            overwrite(parameters.resourceFiles) { invariantSeparatorsPath.split("/").contains(it) }

            if (parameters.translationDir.asFileTree.isEmpty) {
                fileOps.delete { delete(parameters.translationDir) }
            }
        }

        private fun overwrite(
                files: ListProperty<RegularFile>,
                predicate: File.(String) -> Boolean
        ) {
            val translations = parameters.translationDir.asFileTree
            for (file in files.get()) {
                val from = translations.singleOrNull {
                    // Transifex uses a 'foo_bar_resource-name_en.extension' format
                    val normalized = it.nameWithoutExtension.split("_").run { get(lastIndex - 1) }
                    file.asFile.predicate(normalized)
                } ?: continue
                Files.move(from, file.asFile)
            }
        }

        interface Params : WorkParameters {
            val translationDir: DirectoryProperty
            val playListingFiles: ListProperty<RegularFile>
            val resourceFiles: ListProperty<RegularFile>
        }
    }
}
