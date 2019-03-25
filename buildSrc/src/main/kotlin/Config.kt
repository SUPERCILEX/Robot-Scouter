import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.version
import org.gradle.plugin.use.PluginDependenciesSpec

@Suppress("MayBeConstant") // Improve perf when changing values
object Config {
    private const val kotlinVersion = "1.3.30-eap-99"

    fun RepositoryHandler.deps() {
        google().content {
            includeGroupByRegex("com\\.android\\..*")
            includeGroupByRegex("com\\.google\\..*")
            includeGroupByRegex("androidx\\..*")

            includeGroup("com.crashlytics.sdk.android")
            includeGroup("io.fabric.sdk.android")
        }

        maven("https://dl.bintray.com/kotlin/kotlin-dev/").content {
            includeGroup("org.jetbrains.kotlin")
        }

        maven("https://maven.fabric.io/public").content {
            includeGroup("io.fabric.tools")
        }

        maven("https://jitpack.io").content {
            includeGroupByRegex("com.github.SUPERCILEX\\..*")
        }

        jcenter()
    }

    object SdkVersions {
        val compile = 28
        val target = 28
        val min = 16
    }

    object Plugins {
        val android = "com.android.tools.build:gradle:3.5.0-alpha07"
        val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

        val google = "com.google.gms:google-services:4.2.0"
        val firebase = "com.google.firebase:perf-plugin:1.1.5"
        val fabric = "io.fabric.tools:gradle:1.28.1"

        val ktlint = "com.github.shyiko:ktlint:0.31.0"

        val PluginDependenciesSpec.publishing get() = id("com.github.triplet.play") version "2.1.1"
        val PluginDependenciesSpec.versionChecker
            get() = id("com.github.ben-manes.versions") version "0.21.0"
    }

    object Libs {
        object Kotlin {
            private const val coroutinesVersion = "1.1.1"

            val common = "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"
            val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
            val js = "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion"
            val coroutinesAndroid =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
            val coroutinesTasks =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion"
            val coroutinesJs =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion"
        }

        object Anko {
            private const val version = "0.10.8"

            val common = "org.jetbrains.anko:anko-common:$version"
            val coroutines = "org.jetbrains.anko:anko-coroutines:$version"
            val appCompat = "org.jetbrains.anko:anko-appcompat-v7-commons:$version"
            val design = "org.jetbrains.anko:anko-design:$version"
        }

        object Jetpack {
            private const val lifecycleVersion = "2.1.0-alpha03"

            val core = "androidx.core:core-ktx:1.1.0-alpha05"
            val multidex = "androidx.multidex:multidex:2.0.1"
            val appCompat = "androidx.appcompat:appcompat:1.1.0-alpha03"
            val fragment = "androidx.fragment:fragment-ktx:1.1.0-alpha05"
            val rv = "androidx.recyclerview:recyclerview:1.1.0-alpha03"
            val rvSelection = "androidx.recyclerview:recyclerview-selection:1.1.0-alpha01"
            val constraint = "androidx.constraintlayout:constraintlayout:1.1.3"
            val cardView = "androidx.cardview:cardview:1.0.0"
            val palette = "androidx.palette:palette-ktx:1.0.0"
            val emoji = "androidx.emoji:emoji-appcompat:1.0.0"
            val browser = "androidx.browser:browser:1.0.0"
            val pref = "androidx.preference:preference:1.1.0-alpha04"
            val prefKtx = "androidx.preference:preference-ktx:1.1.0-alpha04"

            val material = "com.google.android.material:material:1.1.0-alpha04"

            val common = "androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion"
            val extensions = "androidx.lifecycle:lifecycle-extensions:$lifecycleVersion"
            val liveData = "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion"
            val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion"
            val viewModelState = "androidx.lifecycle:lifecycle-viewmodel-savedstate:1.0.0-alpha01"

            val work = "androidx.work:work-runtime-ktx:2.0.0"
        }

        object Firebase {
            val core = "com.google.firebase:firebase-analytics:16.4.0"
            val auth = "com.google.firebase:firebase-auth:16.2.0"
            val firestore = "com.google.firebase:firebase-firestore:18.1.0"
            val functions = "com.google.firebase:firebase-functions:16.3.0"
            val storage = "com.google.firebase:firebase-storage:16.1.0"
            val config = "com.google.firebase:firebase-config:16.4.1"
            val indexing = "com.google.firebase:firebase-appindexing:17.1.0"
            val messaging = "com.google.firebase:firebase-messaging:17.5.0"
            val invites = "com.google.firebase:firebase-invites:16.1.1"
            val perf = "com.google.firebase:firebase-perf:16.2.4"

            val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.9.9"
        }

        object PlayServices {
            val auth = "com.google.android.gms:play-services-auth:16.0.1"
            val nearby = "com.google.android.gms:play-services-nearby:16.0.0"
            val playCore = "com.google.android.play:core:1.4.0"
        }

        object FirebaseUi {
            private const val version = "aa7a585303"

            val firestore =
                    "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-firestore:$version"
            val auth = "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-auth:$version"
            val facebook = "com.facebook.android:facebook-login:4.41.0"
            val twitter = "com.twitter.sdk.android:twitter-core:3.3.0@aar"
        }

        object Misc {
            private const val leakCanaryVersion = "1.6.3"
            private const val retrofitVersion = "2.5.0"
            private const val poiVersion = "3.17"

            private const val glideVersion = "4.9.0"

            val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
            val leakCanaryFragments =
                    "com.squareup.leakcanary:leakcanary-support-fragment:$leakCanaryVersion"
            val leakCanaryNoop =
                    "com.squareup.leakcanary:leakcanary-android-no-op:$leakCanaryVersion"
            val retrofit = "com.squareup.retrofit2:retrofit:$retrofitVersion"
            val retrofitCoroutines =
                    "com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2"
            val retrofitGson = "com.squareup.retrofit2:converter-gson:$retrofitVersion"
            val gson = "com.google.code.gson:gson:2.8.5"
            val poi = "com.github.SUPERCILEX.poi-android:poi:$poiVersion"
            val poiProguard = "com.github.SUPERCILEX.poi-android:proguard:$poiVersion"

            val glide = "com.github.bumptech.glide:glide:$glideVersion"
            val glideRv = "com.github.bumptech.glide:recyclerview-integration:$glideVersion"
            val flexbox = "com.google.android:flexbox:1.1.0"
            val snap = "com.github.rubensousa:gravitysnaphelper:2.0"
            val permissions = "pub.devrel:easypermissions:3.0.0"
            val mttp = "uk.co.samuelwall:material-tap-target-prompt:2.14.0"
            val billing = "com.android.billingclient:billing:1.2.2"
            val licenses = "net.yslibrary.licenseadapter:licenseadapter:2.2.2"
        }
    }
}
