apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
}

dependencies {
    implementation(project(":shared"))

    implementation(Config.Libs.Support.pref)
    implementation(Config.Libs.Miscellaneous.licenses)
}
