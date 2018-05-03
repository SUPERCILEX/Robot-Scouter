apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
}

dependencies {
    api(project(":shared"))

    implementation(Config.Libs.Support.cardView)
    implementation(Config.Libs.Miscellaneous.snap)
}
