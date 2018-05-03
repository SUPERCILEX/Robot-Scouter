import org.jetbrains.kotlin.gradle.internal.CacheImplementation

apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
    id("kotlin-android-extensions")
}

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    implementation(project(":core"))
    implementation(Config.Libs.Firebase.firestore) { isTransitive = false }
}
