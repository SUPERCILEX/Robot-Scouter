import org.gradle.api.Project

val Project.isReleaseBuild get() = !hasProperty("devBuild")

fun Project.child(name: String) = subprojects.first { it.name == name }
