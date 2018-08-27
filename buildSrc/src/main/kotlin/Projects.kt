import com.supercilex.robotscouter.build.internal.isCi
import com.supercilex.robotscouter.build.internal.isRelease
import org.gradle.api.Project
import org.gradle.api.Task

val Project.buildTags
    get() = listOf(
            if (isCi) "CI" else "Local",
            if (isRelease) "Release" else "Debug",
            if (isReleaseBuild) "ProdBuild" else "DevBuild"
    )

val Project.isReleaseBuild get() = hasProperty("relBuild") || isCi

fun Project.child(name: String) = subprojects.first { it.name == name }

internal fun Project.deepFind(name: String) = deepFind { it.name == name }

internal fun Project.deepFind(spec: (Task) -> Boolean) = allprojects.map {
    it.tasks.matching(spec)
}.flatten()
