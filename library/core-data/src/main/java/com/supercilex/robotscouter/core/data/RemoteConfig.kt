package com.supercilex.robotscouter.core.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.supercilex.robotscouter.core.InvocationMarker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Mirrored in remote_config_defaults.xml
private const val KEY_MINIMUM_APP_VERSION = "minimum_app_version"
private const val KEY_SHOW_RATING_DIALOG = "show_rating_dialog"
private const val KEY_ENABLE_AUTO_SCOUT = "enable_auto_scout"

val minimumAppVersion
    get() = FirebaseRemoteConfig.getInstance().getDouble(KEY_MINIMUM_APP_VERSION).toInt()

val showRatingDialog
    get() = FirebaseRemoteConfig.getInstance().getBoolean(KEY_SHOW_RATING_DIALOG)

val enableAutoScout
    get() = FirebaseRemoteConfig.getInstance().getBoolean(KEY_ENABLE_AUTO_SCOUT)

fun initRemoteConfig() {
    FirebaseRemoteConfig.getInstance().apply {
        setDefaults(R.xml.remote_config_defaults)
        if (BuildConfig.DEBUG) {
            setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
                                           .setMinimumFetchIntervalInSeconds(0)
                                           .build())
        }

        GlobalScope.launch {
            try {
                activate().await()
                fetch().await()
            } catch (e: Exception) {
                throw InvocationMarker(e)
            }
        }
    }
}
