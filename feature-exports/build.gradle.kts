apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
}

dependencies {
    implementation(project(":shared"))

    implementation(Config.Libs.Miscellaneous.poi)
    implementation(Config.Libs.Miscellaneous.poiProguard)
    implementation(Config.Libs.Miscellaneous.gson)
}
