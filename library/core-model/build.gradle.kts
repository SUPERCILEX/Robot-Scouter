import org.jetbrains.kotlin.gradle.internal.CacheImplementation

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    implementation(project(":library:core"))
    compileOnly(Config.Libs.Firebase.firestore) { isTransitive = false }
    compileOnly(Config.Libs.Misc.gson)
}
