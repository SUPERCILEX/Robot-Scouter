object Config {
    private const val kotlinVersion = "1.2.41"

    object SdkVersions {
        const val compile = 28
        const val target = 28
        const val min = 16
    }

    object Plugins {
        const val android = "com.android.tools.build:gradle:3.2.0-alpha17"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"

        const val google = "com.google.gms:google-services:3.2.1"
        const val firebase = "com.google.firebase:firebase-plugins:1.1.5"
        const val fabric = "io.fabric.tools:gradle:1.25.4"
        const val publishing = "com.github.SUPERCILEX:gradle-play-publisher:1c37a81392"

        const val ktlint = "com.github.shyiko:ktlint:0.23.1"
    }

    object Libs {
        object Kotlin {
            const val jvm = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
            const val js = "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion"
            const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:0.22.5"
            const val ktx = "androidx.core:core-ktx:0.3"
        }

        object Anko {
            private const val version = "0.10.5"

            const val common = "org.jetbrains.anko:anko-common:$version"
            const val coroutines = "org.jetbrains.anko:anko-coroutines:$version"
            const val appCompat = "org.jetbrains.anko:anko-appcompat-v7-commons:$version"
            const val design = "org.jetbrains.anko:anko-design:$version"
        }

        object Support {
            private const val version = "27.1.1"

            const val multidex = "com.android.support:multidex:1.0.3"
            const val v4 = "com.android.support:support-v4:$version"
            const val appCompat = "com.android.support:appcompat-v7:$version"
            const val design = "com.android.support:design:$version"
            const val palette = "com.android.support:palette-v7:$version"
            const val cardView = "com.android.support:cardview-v7:$version"
            const val emoji = "com.android.support:support-emoji-appcompat:$version"
            const val customTabs = "com.android.support:support-emoji-appcompat:$version"
            const val pref = "com.android.support:preference-v7:$version"

            const val constraint = "com.android.support.constraint:constraint-layout:1.1.1"
        }

        object Arch {
            private const val version = "1.1.1"
            private const val workVersion = "1.0.0-alpha02"

            const val common = "android.arch.lifecycle:common-java8:$version"
            const val extensions = "android.arch.lifecycle:extensions:$version"
            const val work = "android.arch.work:work-runtime-ktx:$workVersion"
            const val workFirebase = "android.arch.work:work-firebase:$workVersion"
        }

        object Firebase {
            const val core = "com.google.firebase:firebase-core:16.0.0"
            const val auth = "com.google.firebase:firebase-auth:16.0.1"
            const val firestore = "com.google.firebase:firebase-firestore:17.0.1"
            const val functions = "com.google.firebase:firebase-functions:16.0.1"
            const val storage = "com.google.firebase:firebase-storage:16.0.1"
            const val config = "com.google.firebase:firebase-config:16.0.0"
            const val indexing = "com.google.firebase:firebase-appindexing:15.0.1"
            const val messaging = "com.google.firebase:firebase-messaging:17.0.0"
            const val invites = "com.google.firebase:firebase-invites:16.0.0"
            const val perf = "com.google.firebase:firebase-perf:16.0.0"

            const val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.9.2"
        }

        object PlayServices {
            private const val version = "15.0.1"

            const val auth = "com.google.android.gms:play-services-auth:$version"
            const val nearby = "com.google.android.gms:play-services-nearby:$version"
        }

        object FirebaseUi {
            private const val version = "c280098444"

            const val firestore =
                    "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-firestore:$version"
            const val auth = "com.github.SUPERCILEX.FirebaseUI-Android:firebase-ui-auth:$version"
            const val facebook = "com.facebook.android:facebook-login:4.33.0"
            const val twitter = "com.twitter.sdk.android:twitter-core:3.1.1@aar"
        }

        object Misc {
            private const val leakCanaryVersion = "1.5.4"
            private const val retrofitVersion = "2.4.0"
            private const val poiVersion = "3.17"

            private const val glideVersion = "4.7.1"

            const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
            const val leakCanaryNoop =
                    "com.squareup.leakcanary:leakcanary-android-no-op:$leakCanaryVersion"
            const val retrofit = "com.squareup.retrofit2:retrofit:$retrofitVersion"
            const val retrofitGson = "com.squareup.retrofit2:converter-gson:$retrofitVersion"
            const val gson = "com.google.code.gson:gson:2.8.4"
            const val poi = "com.github.SUPERCILEX.poi-android:poi:$poiVersion"
            const val poiProguard = "com.github.SUPERCILEX.poi-android:proguard:$poiVersion"

            const val glide = "com.github.bumptech.glide:glide:$glideVersion"
            const val glideRv = "com.github.bumptech.glide:recyclerview-integration:$glideVersion"
            const val snap = "com.github.rubensousa:gravitysnaphelper:1.5"
            const val permissions = "pub.devrel:easypermissions:1.2.0"
            const val mttp = "uk.co.samuelwall:material-tap-target-prompt:2.9.0"
            const val billing = "com.android.billingclient:billing:1.1"
            const val licenses = "net.yslibrary.licenseadapter:licenseadapter:2.0.2"
        }
    }
}
