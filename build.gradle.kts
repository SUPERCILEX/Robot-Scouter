import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension
import org.jetbrains.kotlin.gradle.internal.CacheImplementation

buildscript {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.fabric.io/public") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev/") }
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

plugins {
    id("com.supercilex.robotscouter.build")
    Config.Plugins.run {
        scan
        versionChecker
    }
}

buildScan {
    setServer("https://svewtxrrdtkawq4bb45ov6wnha-trial.gradle.com")
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")

    publishAlways()
    for (tag in buildTags) tag(tag)
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev/") }
    }

    configureGeneral()
    configureAndroid()
}

tasks.withType<Wrapper> {
    distributionType = Wrapper.DistributionType.ALL
}

fun Project.configureGeneral() {
    val ktlintConfig = configurations.create("ktlint")
    val ktlint = tasks.register<JavaExec>("ktlint") {
        main = "com.github.shyiko.ktlint.Main"
        classpath = ktlintConfig
        args = listOf("src/**/*.kt")
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(ktlint) }

    dependencies { "ktlint"(Config.Plugins.ktlint) }
}

fun Project.configureAndroid() {
    // Resource packaging breaks otherwise for some reason
    tasks.matching { it.name.contains("Test") }.configureEach { enabled = false }

    val tree = (group as String).split(".")
    when {
        tree.contains("library") && name != "common" -> apply(plugin = "com.android.library")
        name == "android-base" -> apply(plugin = "com.android.application")
        tree.contains("feature") -> apply(plugin = "com.android.dynamic-feature")
        else -> return
    }
    apply(plugin = "kotlin-android")
    apply(plugin = "kotlin-android-extensions")

    configure<BaseExtension> {
        compileSdkVersion(Config.SdkVersions.compile)

        defaultConfig {
            minSdkVersion(Config.SdkVersions.min)
            targetSdkVersion(Config.SdkVersions.target)

            versionCode = 1
            vectorDrawables.useSupportLibrary = true
        }

        lintOptions {
            isCheckAllWarnings = true
            isWarningsAsErrors = true
            isAbortOnError = false

            baseline(file("$rootDir/app/android-base/lint-baseline.xml"))
            disable(
                    "InvalidPackage", // Needed because of Okio
                    "GradleDependency", "NewerVersionAvailable" // For build reproducibility
            )

            val reportsDir = "$buildDir/reports"
            htmlOutput = file("$reportsDir/lint-results.html")
            xmlOutput = file("$reportsDir/lint-results.xml")
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
        configure(delegateClosureOf<AndroidExtensionsExtension> { isExperimental = true })
        defaultCacheImplementation = CacheImplementation.SPARSE_ARRAY
    }
}
