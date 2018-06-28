import org.jetbrains.kotlin.gradle.internal.CacheImplementation
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
    serviceAccountCredentials = file("google-play-auto-publisher.json")
    track = "alpha"
    resolutionStrategy = "auto"
    outputProcessor = { versionNameOverride = "$versionNameOverride.$versionCode" }
}

dependencies {
    implementation(project(":library:shared"))

    implementation(Config.Libs.Support.multidex)
    implementation(Config.Libs.PlayServices.playCore)
    implementation(Config.Libs.Misc.billing)

    implementation(Config.Libs.Firebase.perf)
    implementation(Config.Libs.Firebase.invites)

    // TODO https://issuetracker.google.com/issues/110012194
    implementation(project(":library:shared-scouting"))
    // TODO remove when Firebase updates their deps
    implementation("com.google.firebase:firebase-iid:16.2.0")
    implementation(Config.Libs.Misc.gson) // Override Firestore
}

apply(plugin = "com.google.gms.google-services")
