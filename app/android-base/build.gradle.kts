import com.google.gms.googleservices.GoogleServicesPlugin
import org.jetbrains.kotlin.gradle.internal.CacheImplementation
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("io.fabric")
    id("com.github.triplet.play")
}
if (isReleaseBuild) apply(plugin = "com.google.firebase.firebase-perf")
crashlytics.alwaysUpdateBuildId = isReleaseBuild

android {
    dynamicFeatures = rootProject.project("feature").childProjects.mapTo(mutableSetOf()) {
        ":feature:${it.key}"
    }

    defaultConfig {
        applicationId = "com.supercilex.robotscouter"
        versionName = "3.0.0-beta1"
        multiDexEnabled = true
    }

    signingConfigs {
        register("release") {
            val keystorePropertiesFile = file(if (isReleaseBuild) {
                "upload-keystore.properties"
            } else {
                "keystore.properties"
            })

            if (!keystorePropertiesFile.exists()) {
                logger.warn("Release builds may not work: signing config not found.")
                return@register
            }

            val keystoreProperties = Properties()
            keystoreProperties.load(FileInputStream(keystorePropertiesFile))

            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        named("debug").configure {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        named("release").configure {
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
    defaultToAppBundles = true

    resolutionStrategy = "auto"
    outputProcessor = { versionNameOverride = "$versionNameOverride.$versionCode" }
}

dependencies {
    implementation(project(":library:shared"))
    implementation(project(":library:shared-scouting"))

    implementation(Config.Libs.Jetpack.multidex)
    implementation(Config.Libs.PlayServices.playCore)
    implementation(Config.Libs.Misc.billing)

    implementation(Config.Libs.Firebase.perf)
    implementation(Config.Libs.Firebase.invites)
}

apply(plugin = "com.google.gms.google-services")
GoogleServicesPlugin.config.disableVersionCheck = true
