import org.jetbrains.kotlin.gradle.internal.CacheImplementation

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    implementation(project(":library:core"))
    implementation(Config.Libs.Firebase.firestore) { isTransitive = false }
}
