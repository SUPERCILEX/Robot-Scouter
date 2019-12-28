@file:Suppress("KDocMissingDocumentation", "PublicApiImplicitType")

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.version
import org.gradle.plugin.use.PluginDependenciesSpec

fun RepositoryHandler.deps() {
    google().content {
        includeGroupByRegex("com\\.android\\..*")
        includeGroupByRegex("com\\.google\\..*")
        includeGroupByRegex("androidx\\..*")

        includeGroup("com.android")
        includeGroup("com.crashlytics.sdk.android")
        includeGroup("io.fabric.sdk.android")
    }

    maven("https://maven.fabric.io/public").content {
        includeGroup("io.fabric.tools")
    }

    maven("https://jitpack.io").content {
        includeGroupByRegex("com.github.SUPERCILEX\\..*")
    }

    jcenter()
}

object Config {
    private const val kotlinVersion = "1.3.61"

    object SdkVersions {
        const val compile = 29
        const val target = 28
        const val min = 16
    }

    object Plugins {
        const val android = "com.android.tools.build:gradle:4.0.0-alpha07"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

        const val google = "com.google.gms:google-services:4.3.3"
        const val firebase = "com.google.firebase:perf-plugin:1.3.1"
        const val fabric = "io.fabric.tools:gradle:1.31.2"

        const val ktlint = "com.pinterest:ktlint:0.33.0"

        val PluginDependenciesSpec.publishing get() = id("com.github.triplet.play") version "2.6.2"
        val PluginDependenciesSpec.versionChecker
            get() = id("com.github.ben-manes.versions") version "0.27.0"
    }

    object Libs {
        object Kotlin {
            private const val coroutinesVersion = "1.3.3"

            const val common = "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"
            const val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
            const val js = "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion"
            const val coroutinesAndroid =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
            const val coroutinesTasks =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion"
            const val coroutinesJs =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion"
        }

        object Jetpack {
            const val core = "androidx.core:core-ktx:1.2.0-rc01"
            const val multidex = "androidx.multidex:multidex:2.0.1"
            const val appCompat = "androidx.appcompat:appcompat:1.2.0-alpha01"
            const val fragment = "androidx.fragment:fragment-ktx:1.2.0-rc04"
            const val rv = "androidx.recyclerview:recyclerview:1.2.0-alpha01"
            const val rvSelection = "androidx.recyclerview:recyclerview-selection:1.1.0-beta01"
            const val constraint = "androidx.constraintlayout:constraintlayout:1.1.3"
            const val cardView = "androidx.cardview:cardview:1.0.0"
            const val palette = "androidx.palette:palette-ktx:1.0.0"
            const val emoji = "androidx.emoji:emoji-appcompat:1.0.0"
            const val browser = "androidx.browser:browser:1.2.0"
            const val pref = "androidx.preference:preference-ktx:1.1.0"

            const val material = "com.google.android.material:material:1.2.0-alpha03"

            val lifecycle by lazy {
                val version = "2.2.0-rc03"
                listOf(
                        "androidx.lifecycle:lifecycle-common-java8:$version",
                        "androidx.lifecycle:lifecycle-extensions:$version",
                        "androidx.lifecycle:lifecycle-livedata-ktx:$version",
                        "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
                )
            }
            const val viewModelState =
                    "androidx.lifecycle:lifecycle-viewmodel-savedstate:1.0.0-rc03"

            val work by lazy {
                val version = "2.3.0-beta02"
                listOf(
                        "androidx.work:work-runtime-ktx:$version",
                        "androidx.work:work-gcm:$version"
                )
            }
        }

        object Firebase {
            const val core = "com.google.firebase:firebase-analytics:17.2.1"
            const val auth = "com.google.firebase:firebase-auth:19.2.0"
            const val firestore = "com.google.firebase:firebase-firestore-ktx:21.3.1"
            const val functions = "com.google.firebase:firebase-functions-ktx:19.0.1"
            const val storage = "com.google.firebase:firebase-storage-ktx:19.1.0"
            const val config = "com.google.firebase:firebase-config-ktx:19.1.0"
            const val indexing = "com.google.firebase:firebase-appindexing:19.0.0"
            const val messaging = "com.google.firebase:firebase-messaging:20.1.0"
            const val invites = "com.google.firebase:firebase-invites:17.0.0"
            const val perf = "com.google.firebase:firebase-perf:19.0.4"

            const val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.10.1"
        }

        object PlayServices {
            const val auth = "com.google.android.gms:play-services-auth:17.0.0"
            const val nearby = "com.google.android.gms:play-services-nearby:17.0.0"
            const val playCore = "com.google.android.play:core-ktx:1.6.4"
        }

        object FirebaseUi {
            private const val version = "aa7a585303"

            const val firestore =
                    "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-firestore:$version"
            const val auth = "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-auth:$version"
            const val facebook = "com.facebook.android:facebook-login:5.13.0"
            const val twitter = "com.twitter.sdk.android:twitter-core:3.3.0@aar"
        }

        object Misc {
            private const val retrofitVersion = "2.7.0"
            private const val poiVersion = "3.17"

            private const val glideVersion = "4.10.0"

            const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.0"
            const val retrofit = "com.squareup.retrofit2:retrofit:$retrofitVersion"
            const val retrofitGson = "com.squareup.retrofit2:converter-gson:$retrofitVersion"
            const val gson = "com.google.code.gson:gson:2.8.6"
            const val poi = "com.github.SUPERCILEX.poi-android:poi:$poiVersion"
            const val poiProguard = "com.github.SUPERCILEX.poi-android:proguard:$poiVersion"

            const val glide = "com.github.bumptech.glide:glide:$glideVersion"
            const val glideRv = "com.github.bumptech.glide:recyclerview-integration:$glideVersion"
            const val flexbox = "com.google.android:flexbox:2.0.0"
            const val snap = "com.github.rubensousa:gravitysnaphelper:2.2.0"
            const val permissions = "pub.devrel:easypermissions:3.0.0"
            const val mttp = "uk.co.samuelwall:material-tap-target-prompt:3.0.0"
            const val licenses = "net.yslibrary.licenseadapter:licenseadapter:3.0.0"
        }
    }
}
