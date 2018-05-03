apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
}

dependencies {
    implementation(project(":shared-scouting"))

    implementation(Config.Libs.Support.palette)
}
