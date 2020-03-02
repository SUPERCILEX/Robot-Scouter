dependencies {
    api(project(":library:core-data"))
    api(project(":library:core-ui"))

    api(Config.Libs.FirebaseUi.auth)
    api(Config.Libs.PlayServices.auth)
    api(Config.Libs.Misc.glide)

    implementation(Config.Libs.FirebaseUi.facebook)
    implementation(Config.Libs.FirebaseUi.twitter) { isTransitive = true }
    implementation(Config.Libs.Firebase.links)
}
