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

    Config.Libs.Jetpack.lifecycle.forEach { api(it) }
    api(Config.Libs.Jetpack.viewModelState)

    implementation(Config.Libs.Anko.common)
    implementation(Config.Libs.Jetpack.appCompat) { isTransitive = false }
    implementation(Config.Libs.Jetpack.pref)
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
