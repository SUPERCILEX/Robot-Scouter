import org.gradle.api.Project

val Project.isReleaseBuild get() = !hasProperty("devBuild")
