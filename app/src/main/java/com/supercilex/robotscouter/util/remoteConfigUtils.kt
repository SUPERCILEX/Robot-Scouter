package com.supercilex.robotscouter.util

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.supercilex.robotscouter.BuildConfig
import com.supercilex.robotscouter.R
import java.util.concurrent.TimeUnit

fun initRemoteConfig() {
    FirebaseRemoteConfig.getInstance().apply {
        setDefaults(R.xml.remote_config_defaults)
        setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                                  .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                  .build())
    }
}

fun fetchAndActivate(): Task<Nothing?> {
    val config: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    val cacheExpiration: Long = if (config.info.configSettings.isDeveloperModeEnabled) {
        0
    } else {
        TimeUnit.HOURS.toSeconds(12)
    }

    val activate = TaskCompletionSource<Nothing?>()
    config.fetch(cacheExpiration).addOnCompleteListener {
        FirebaseRemoteConfig.getInstance().activateFetched()
        activate.setResult(null)
    }

    return activate.task
}
