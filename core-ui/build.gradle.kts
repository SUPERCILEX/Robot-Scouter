import org.jetbrains.kotlin.gradle.internal.CacheImplementation

apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
    id("kotlin-android-extensions")
}

android {
    buildTypes {
        getByName("release") {
            postprocessing {
                consumerProguardFile("proguard-rules.pro")
            }
        }
    }
}

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    api(project(":core"))

    api(Config.Libs.Support.design)
    api(Config.Libs.Support.emoji)
    api(Config.Libs.Support.constraint)
    api(Config.Libs.FirebaseUi.firestore)
    api(Config.Libs.Anko.design)
    api(Config.Libs.Anko.appCompat)

    implementation(Config.Libs.Support.customTabs)
    implementation(Config.Libs.Support.pref)

    // Needed for override
    implementation(Config.Libs.Firebase.firestore)
}
