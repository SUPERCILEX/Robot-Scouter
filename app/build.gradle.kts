import org.jetbrains.kotlin.gradle.internal.CacheImplementation
import java.io.FileInputStream
import java.util.Properties

apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.application")
    id("io.fabric")
    id("com.github.triplet.play")
}
if (!project.hasProperty("devBuild")) apply(plugin = "com.google.firebase.firebase-perf")
tasks.whenTaskAdded {
    if (name == "assembleRelease") dependsOn("updateReleasePlayVersion")
}

android {
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
            extra["alwaysUpdateBuildId"] = false
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
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
    setTrack("alpha")
    serviceAccountEmail = "google-play-auto-publisher@robot-scouter-app.iam.gserviceaccount.com"
    jsonFile = file("google-play-auto-publisher.json")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":feature-teams"))
    implementation(project(":feature-autoscout"))
    implementation(project(":feature-scouts"))
    implementation(project(":feature-templates"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-exports"))

    implementation(Config.Libs.Support.multidex)
    implementation(Config.Libs.Miscellaneous.billing)

    implementation(Config.Libs.Firebase.perf)
    implementation(Config.Libs.Firebase.invites)
    implementation(Config.Libs.Firebase.messaging)
}

apply(plugin = "com.google.gms.google-services")
