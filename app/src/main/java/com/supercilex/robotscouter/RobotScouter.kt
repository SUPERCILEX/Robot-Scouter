package com.supercilex.robotscouter

import android.content.Context
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.supercilex.robotscouter.data.client.spreadsheet.SpreadsheetUtils
import com.supercilex.robotscouter.util.DatabaseHelper
import com.supercilex.robotscouter.util.initAnalytics
import com.supercilex.robotscouter.util.initConstants

class RobotScouter : MultiDexApplication() {
    private val refWatcher: RefWatcher by lazy { LeakCanary.install(this) }

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }

        initConstants(this)
        DatabaseHelper.init(this)
        initAnalytics(this)
        SpreadsheetUtils.init(this)

        FirebaseRemoteConfig.getInstance().apply {
            setDefaults(R.xml.remote_config_defaults)
            setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(BuildConfig.DEBUG)
                    .build())
        }
    }

    companion object {
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
        }

        fun getRefWatcher(context: Context): RefWatcher = (context.applicationContext as RobotScouter).refWatcher
    }
}
