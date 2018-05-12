import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jetbrains.kotlin.gradle.internal.CacheImplementation

buildscript {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.fabric.io/public") }
    }

    dependencies {
        classpath(Config.Plugins.android)
        classpath(Config.Plugins.kotlin)
        classpath(Config.Plugins.google)
        classpath(Config.Plugins.firebase)
        classpath(Config.Plugins.fabric)
        classpath(Config.Plugins.publishing)
    }
}

// See https://github.com/gradle/kotlin-dsl/issues/607#issuecomment-375687119
subprojects { parent!!.path.takeIf { it != rootProject.path }?.let { evaluationDependsOn(it) } }

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
    }

    configureGeneral()
    configureAndroid()
}

fun Project.configureGeneral() {
    configurations { create("ktlint") }
    task("ktlint", JavaExec::class) {
        main = "com.github.shyiko.ktlint.Main"
        classpath = configurations.getByName("ktlint")
        args = listOf("src/**/*.kt")
    }
    tasks.whenTaskAdded {
        if (name == "check") dependsOn("ktlint")
    }

    dependencies { "ktlint"(Config.Plugins.ktlint) }
}

fun Project.configureAndroid() {
    // Resource packaging breaks otherwise for some reason
    tasks.whenTaskAdded {
        if (name.contains("Test")) enabled = false
    }

    val tree = (project.group as String).split(".")
    when {
        project.name == "app" -> "com.android.application"
        tree.contains("feature") || tree.contains("library") -> "com.android.library"
        else -> null
    }?.let {
        apply(plugin = it)
        apply(plugin = "kotlin-android")
        apply(plugin = "kotlin-android-extensions")

        configure<BaseExtension> {
            compileSdkVersion(Config.SdkVersions.compile)

            defaultConfig {
                minSdkVersion(Config.SdkVersions.min)
                targetSdkVersion(Config.SdkVersions.target)

                versionCode = 1
                resConfigs("en")
                vectorDrawables.useSupportLibrary = true
            }

            lintOptions {
                isCheckAllWarnings = true
                isWarningsAsErrors = true
                isAbortOnError = false

                baseline(file("${project.rootDir}/app/lint-baseline.xml"))
                disable(
                        "InvalidPackage", // Needed because of Okio
                        "GradleDependency", "NewerVersionAvailable" // For build reproducibility
                )

                val reportsDir = "${project.buildDir}/reports"
                setHtmlOutput(file("$reportsDir/lint-results.html"))
                setXmlOutput(file("$reportsDir/lint-results.xml"))
            }

            compileOptions {
                setSourceCompatibility(JavaVersion.VERSION_1_8)
                setTargetCompatibility(JavaVersion.VERSION_1_8)
            }
        }

        configure<KotlinProjectExtension> {
            experimental.coroutines = Coroutines.ENABLE
        }

        configure<AndroidExtensionsExtension> {
            isExperimental = true
            defaultCacheImplementation = CacheImplementation.SPARSE_ARRAY
        }
        apply(from = "$rootDir/kotlin-extensions-bug.gradle")
    }
}
