import org.jetbrains.kotlin.gradle.internal.CacheImplementation
import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("io.fabric")
    id("com.github.triplet.play")
}
if (!project.hasProperty("devBuild")) apply(plugin = "com.google.firebase.firebase-perf")

android {
    dynamicFeatures = rootProject.project("feature").childProjects.mapTo(mutableSetOf()) {
        ":feature:${it.key}"
    }

    defaultConfig {
        applicationId = "com.supercilex.robotscouter"
        versionName = "2.3.1"
        multiDexEnabled = true
        manifestPlaceholders = mapOf("appName" to "@string/app_name")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = file("keystore.properties")
            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))

            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            manifestPlaceholders = mapOf("appName" to "Robot Scouter DEBUG")
            crashlytics.alwaysUpdateBuildId = false
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            setProguardFiles(listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
            ))

            // TODO Crashlytics doesn't support the new DSL yet so we need to downgrade
//            postprocessing {
//                removeUnusedCode true
//                removeUnusedResources true
//                obfuscate true
//                optimizeCode true
//                proguardFile 'proguard-rules.pro'
//            }
        }
    }
}

play {
    track = "alpha"
    resolutionStrategy = "auto"
    versionNameOverride = { "${android.defaultConfig.versionName}.$it" }
    jsonFile = file("google-play-auto-publisher.json")
}

dependencies {
    api(project(":library:shared"))

    implementation(Config.Libs.Support.multidex)
    implementation(Config.Libs.PlayServices.playCore)
    implementation(Config.Libs.Misc.billing)

    implementation(Config.Libs.Firebase.perf)
    implementation(Config.Libs.Firebase.invites)
    implementation(Config.Libs.Firebase.messaging)

    // TODO https://issuetracker.google.com/issues/110012194
    implementation(project(":library:shared-scouting"))
}

apply(plugin = "com.google.gms.google-services")
