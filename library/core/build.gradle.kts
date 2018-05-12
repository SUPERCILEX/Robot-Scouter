import org.jetbrains.kotlin.gradle.internal.CacheImplementation

plugins {
    id("kotlin-platform-android")
}

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    api(project(":common"))
    expectedBy(project(":common"))

    api(Config.Libs.Kotlin.jvm)
    api(Config.Libs.Kotlin.coroutines)
    api(Config.Libs.Anko.coroutines)
    api(Config.Libs.Firebase.analytics)
    api(Config.Libs.Firebase.crashlytics)
    debugApi(Config.Libs.Miscellaneous.leakCanary)
    releaseApi(Config.Libs.Miscellaneous.leakCanaryNoop)
    api(Config.Libs.Kotlin.ktx)

    implementation(Config.Libs.Anko.common)
    implementation(Config.Libs.Firebase.firestore) { isTransitive = true }

    // Needed for override
    api(Config.Libs.Support.v4)
}
