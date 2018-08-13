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
    api(Config.Libs.Firebase.iid)
    api(Config.Libs.Firebase.crashlytics)
    debugApi(Config.Libs.Misc.leakCanary)
    debugApi(Config.Libs.Misc.leakCanaryFragments)
    releaseApi(Config.Libs.Misc.leakCanaryNoop)
    api(Config.Libs.Jetpack.core)
}
