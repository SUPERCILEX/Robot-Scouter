import com.supercilex.robotscouter.build.internal.isCi
import com.supercilex.robotscouter.build.internal.isRelease
import org.gradle.api.Project

val Project.buildTags: List<String>
    get() = listOf(
            if (isCi) "CI" else "Local",
            if (isRelease) "Release" else "Debug",
            if (isReleaseBuild) "ProdBuild" else "DevBuild"
    )

val Project.isReleaseBuild: Boolean get() = hasProperty("relBuild") || isCi

internal fun Project.child(name: String): Project = subprojects.first { it.name == name }
