import org.jetbrains.kotlin.gradle.internal.CacheImplementation

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    implementation(project(":library:common"))
    implementation(Config.Libs.Jetpack.core)

    compileOnly(Config.Libs.Firebase.firestore) { exclude(group = "com.google.guava") }
    compileOnly(Config.Libs.Misc.gson)
}
