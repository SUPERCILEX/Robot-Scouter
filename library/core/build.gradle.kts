import org.jetbrains.kotlin.gradle.internal.CacheImplementation

plugins {
    id("kotlin-platform-android")
}

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    api(project(":library:common"))
    expectedBy(project(":library:common"))

    api(Config.Libs.Kotlin.jvm)
    api(Config.Libs.Kotlin.coroutinesAndroid)
    api(Config.Libs.Anko.coroutines)
    api(Config.Libs.Firebase.core)
    api(Config.Libs.Firebase.crashlytics)
    debugApi(Config.Libs.Misc.leakCanary)
    releaseApi(Config.Libs.Misc.leakCanaryNoop)
    api(Config.Libs.Jetpack.core)

    implementation(Config.Libs.Anko.common)
    implementation(Config.Libs.Firebase.firestore) { isTransitive = true }

    // Needed for override
    // TODO remove when Firebase updates their deps
    api(Config.Libs.Support.v4)
    api("com.google.firebase:firebase-iid:16.2.0")
}
