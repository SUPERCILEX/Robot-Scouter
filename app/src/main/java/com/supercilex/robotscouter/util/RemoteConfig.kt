package com.supercilex.robotscouter.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import java.util.concurrent.TimeUnit

// Mirrored in remote_config_defaults.xml
private const val KEY_MINIMUM_APP_VERSION = "minimum_app_version"
private const val KEY_FRESHNESS_DAYS = "team_freshness"
private const val KEY_UPDATE_MESSAGE = "update_required_message"
private const val KEY_SHOW_RATING_DIALOG = "show_rating_dialog"

val minimumAppVersion
    get() = FirebaseRemoteConfig.getInstance().getDouble(KEY_MINIMUM_APP_VERSION).toInt()
val updateRequiredMessage: String
    get() = FirebaseRemoteConfig.getInstance().getString(KEY_UPDATE_MESSAGE)
val teamFreshnessDays
    get() = FirebaseRemoteConfig.getInstance().getDouble(KEY_FRESHNESS_DAYS).toLong()

val showRatingDialog
    get() = FirebaseRemoteConfig.getInstance().getBoolean(KEY_SHOW_RATING_DIALOG)

fun initRemoteConfig() {
    FirebaseRemoteConfig.getInstance().apply {
        setDefaults(R.xml.remote_config_defaults)
        setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                                  .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                  .build())
    }
}

suspend fun fetchAndActivate() {
    val config: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    val cacheExpiration: Long = if (config.info.configSettings.isDeveloperModeEnabled) {
        0
    } else {
        TimeUnit.HOURS.toSeconds(12)
    }

    try {
        config.fetch(cacheExpiration).await()
    } catch (e: FirebaseRemoteConfigFetchException) {
        // Ignore throttling since it shouldn't happen on release builds and isn't really a public
        // API error.
        return
    } catch (e: Exception) {
        CrashLogger.onFailure(e)
        return
    }

    FirebaseRemoteConfig.getInstance().activateFetched()
}
