package com.supercilex.robotscouter

import android.content.Context
import android.support.multidex.MultiDexApplication
import com.google.firebase.perf.metrics.AddTrace
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.supercilex.robotscouter.util.AnalyticsUtils
import com.supercilex.robotscouter.util.Constants
import com.supercilex.robotscouter.util.DatabaseHelper

class RobotScouter : MultiDexApplication() {
    private val refWatcher: RefWatcher by lazy { LeakCanary.install(this) }

    @AddTrace(name = "onCreate")
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        Constants.init(this)
        DatabaseHelper.init(this)
        AnalyticsUtils.init(this)

        FirebaseRemoteConfig.getInstance().apply {
            setDefaults(R.xml.remote_config_defaults)
            setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(BuildConfig.DEBUG)
                    .build())
        }
    }

    companion object {
        fun getRefWatcher(context: Context): RefWatcher = (context.applicationContext as RobotScouter).refWatcher
    }
}
