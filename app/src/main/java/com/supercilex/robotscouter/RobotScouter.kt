package com.supercilex.robotscouter

import android.os.Build
import android.os.StrictMode
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

@Suppress("PropertyName")
val RobotScouter
    get() = app

private var app: RobotScouterApp by LateinitVal()

class RobotScouterApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return

        app = this

        initAnalytics()
        initRemoteConfig()
        initDatabase()
        initPrefs()
        initUi()
        initNotifications()

        if (BuildConfig.DEBUG) {
            // Purposefully put this after initialization since Google is terrible with disk I/O.
            val vmBuilder = StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vmBuilder.detectCleartextNetwork()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vmBuilder.detectContentUriWithoutPermission()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                vmBuilder.detectFileUriExposure()
            }
            StrictMode.setVmPolicy(vmBuilder.penaltyLog().build())

            StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyFlashScreen()
                            .penaltyLog()
                            .build()
            )
        }
    }

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
