import org.gradle.kotlin.dsl.version
import org.gradle.plugin.use.PluginDependenciesSpec

@Suppress("MayBeConstant") // Improve perf when changing values
object Config {
    private const val kotlinVersion = "1.2.71"

    object SdkVersions {
        val compile = 28
        val target = 28
        val min = 16
    }

    object Plugins {
        val android = "com.android.tools.build:gradle:3.3.0-alpha11"
        val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

        val google = "com.google.gms:google-services:4.0.2"
        val firebase = "com.google.firebase:firebase-plugins:1.1.5"
        val fabric = "io.fabric.tools:gradle:1.26.0"
        val publishing = "com.github.Triple-T:gradle-play-publisher:7829c8646f"

        val ktlint = "com.github.shyiko:ktlint:0.28.0"

        val PluginDependenciesSpec.versionChecker get() = id("com.github.ben-manes.versions") version "0.20.0"
    }

    object Libs {
        object Kotlin {
            private const val coroutinesVersion = "0.26.1"

            val common = "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion"
            val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
            val js = "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion"
            val coroutinesAndroid =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
            // TODO figure out JS bugs
            val coroutinesJs =
                    "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:0.24.0"
        }

        object Anko {
            private const val version = "0.10.6"

            val common = "org.jetbrains.anko:anko-common:$version"
            val coroutines = "org.jetbrains.anko:anko-coroutines:$version"
            val appCompat = "org.jetbrains.anko:anko-appcompat-v7-commons:$version"
            val design = "org.jetbrains.anko:anko-design:$version"
        }

        object Jetpack {
            private const val version = "1.0.0"
            private const val lifecycleVersion = "2.0.0"
            private const val workVersion = "1.0.0-alpha08"

            val core = "androidx.core:core-ktx:$version"
            val multidex = "androidx.multidex:multidex:2.0.0"
            val appCompat = "androidx.appcompat:appcompat:$version"
            val fragment = "androidx.fragment:fragment-ktx:$version"
            val rvSelection = "androidx.recyclerview:recyclerview-selection:$version"
            val constraint = "androidx.constraintlayout:constraintlayout:1.1.3"
            val cardView = "androidx.cardview:cardview:$version"
            val palette = "androidx.palette:palette-ktx:$version"
            val emoji = "androidx.emoji:emoji-appcompat:$version"
            val browser = "androidx.browser:browser:$version"
            val pref = "androidx.preference:preference:$version"
            val prefKtx = "androidx.preference:preference-ktx:$version"

            val material = "com.google.android.material:material:1.0.0"

            val common = "androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion"
            val extensions = "androidx.lifecycle:lifecycle-extensions:$lifecycleVersion"
            val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion"

            val work = "android.arch.work:work-runtime-ktx:$workVersion"
            val workFirebase = "android.arch.work:work-firebase:$workVersion"
        }

        object Firebase {
            val core = "com.google.firebase:firebase-core:16.0.3"
            val auth = "com.google.firebase:firebase-auth:16.0.3"
            val firestore = "com.google.firebase:firebase-firestore:17.1.0"
            val functions = "com.google.firebase:firebase-functions:16.1.0"
            val storage = "com.google.firebase:firebase-storage:16.0.2"
            val config = "com.google.firebase:firebase-config:16.0.0"
            val indexing = "com.google.firebase:firebase-appindexing:16.0.1"
            val messaging = "com.google.firebase:firebase-messaging:17.3.2"
            val invites = "com.google.firebase:firebase-invites:16.0.3"
            val perf = "com.google.firebase:firebase-perf:16.1.0"

            val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.9.5"
        }

        object PlayServices {
            val auth = "com.google.android.gms:play-services-auth:16.0.0"
            val nearby = "com.google.android.gms:play-services-nearby:15.0.1"
            val playCore = "com.google.android.play:core:1.3.4"
        }

        object FirebaseUi {
            private const val version = "60f451cfea"

            val firestore =
                    "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-firestore:$version"
            val auth = "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-auth:$version"
            val facebook = "com.facebook.android:facebook-login:4.36.1"
            val twitter = "com.twitter.sdk.android:twitter-core:3.3.0@aar"
        }

        object Misc {
            private const val leakCanaryVersion = "1.6.1"
            private const val retrofitVersion = "2.4.0"
            private const val poiVersion = "3.17"

            private const val glideVersion = "4.8.0"

            val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
            val leakCanaryFragments =
                    "com.squareup.leakcanary:leakcanary-support-fragment:$leakCanaryVersion"
            val leakCanaryNoop =
                    "com.squareup.leakcanary:leakcanary-android-no-op:$leakCanaryVersion"
            val retrofit = "com.squareup.retrofit2:retrofit:$retrofitVersion"
            val retrofitGson = "com.squareup.retrofit2:converter-gson:$retrofitVersion"
            val gson = "com.google.code.gson:gson:2.8.5"
            val poi = "com.github.SUPERCILEX.poi-android:poi:$poiVersion"
            val poiProguard = "com.github.SUPERCILEX.poi-android:proguard:$poiVersion"

            val glide = "com.github.bumptech.glide:glide:$glideVersion"
            val glideRv = "com.github.bumptech.glide:recyclerview-integration:$glideVersion"
            val snap = "com.github.rubensousa:gravitysnaphelper:1.5"
            val permissions = "pub.devrel:easypermissions:1.3.0"
            val mttp = "uk.co.samuelwall:material-tap-target-prompt:2.12.4"
            val billing = "com.android.billingclient:billing:1.1"
            val licenses = "net.yslibrary.licenseadapter:licenseadapter:2.1.2"
        }
    }
}
