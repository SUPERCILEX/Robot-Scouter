dependencies {
    api(project(":library:core-data"))
    api(project(":library:core-ui"))

    api(Config.Libs.FirebaseUi.auth)
    api(Config.Libs.Miscellaneous.glide)
    api(Config.Libs.Miscellaneous.permissions)

    implementation(Config.Libs.FirebaseUi.facebook)
    implementation(Config.Libs.FirebaseUi.twitter) { isTransitive = true }
    implementation(Config.Libs.Firebase.invites)

    // Needed for override
    api(Config.Libs.PlayServices.auth)
    api(Config.Libs.Support.cardView)
    api(Config.Libs.Support.customTabs)
}
