package com.supercilex.robotscouter

import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import com.squareup.leakcanary.LeakCanary
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.initDatabase
import com.supercilex.robotscouter.util.data.initPrefs
import com.supercilex.robotscouter.util.initAnalytics
import com.supercilex.robotscouter.util.initRemoteConfig
import com.supercilex.robotscouter.util.ui.initNotifications
import com.supercilex.robotscouter.util.ui.initUi

class RobotScouter : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return

        INSTANCE = this

        initAnalytics()
        initRemoteConfig()
        initDatabase()
        initPrefs()
        initUi()
        initNotifications()
    }

    companion object {
        var INSTANCE: RobotScouter by LateinitVal()
            private set

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
