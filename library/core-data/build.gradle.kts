import org.jetbrains.kotlin.gradle.internal.CacheImplementation

androidExtensions {
    defaultCacheImplementation = CacheImplementation.NONE
}

dependencies {
    api(project(":library:core"))
    api(project(":library:core-model"))

    api(Config.Libs.Firebase.firestore)
    api(Config.Libs.Firebase.auth)
    api(Config.Libs.Firebase.indexing)

    api(Config.Libs.Jetpack.common)
    api(Config.Libs.Jetpack.extensions)
    api(Config.Libs.Jetpack.liveData)
    api(Config.Libs.Jetpack.viewModel)

    implementation(Config.Libs.Anko.common)
    implementation(Config.Libs.Jetpack.appCompat) { isTransitive = false }
    implementation(Config.Libs.Jetpack.pref) { isTransitive = false }
    implementation(Config.Libs.Misc.glide) { isTransitive = false }

    implementation(Config.Libs.PlayServices.auth) { isTransitive = false }
    implementation(Config.Libs.FirebaseUi.firestore) { exclude(module = "recyclerview-v7") }
    implementation(Config.Libs.Firebase.functions)
    implementation(Config.Libs.Firebase.storage)
    implementation(Config.Libs.Firebase.messaging)
    implementation(Config.Libs.Firebase.config)
    implementation(Config.Libs.Jetpack.work)

    implementation(Config.Libs.Misc.retrofit)
    implementation(Config.Libs.Misc.retrofitGson)
}
