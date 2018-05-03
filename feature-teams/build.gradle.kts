apply(from = "../standard-android-config.gradle")
plugins {
    id("com.android.library")
}

dependencies {
    implementation(project(":shared"))

    implementation(Config.Libs.Firebase.invites)

    implementation(Config.Libs.Miscellaneous.glideRv)
    implementation(Config.Libs.Miscellaneous.mttp)
}
