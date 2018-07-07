dependencies {
    implementation(project(":app:android-base"))
    implementation(project(":library:shared"))

    implementation(Config.Libs.Jetpack.pref)
    implementation(Config.Libs.Jetpack.prefKtx)
    implementation(Config.Libs.Misc.licenses)
}
