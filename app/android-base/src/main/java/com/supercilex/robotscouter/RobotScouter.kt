package com.supercilex.robotscouter

import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.google.android.play.core.splitcompat.SplitCompat
import com.squareup.leakcanary.LeakCanary
import com.supercilex.robotscouter.core._globalContext
import com.supercilex.robotscouter.core.data.initAnalytics
import com.supercilex.robotscouter.core.data.initDatabase
import com.supercilex.robotscouter.core.data.initIo
import com.supercilex.robotscouter.core.data.initNotifications
import com.supercilex.robotscouter.core.data.initPrefs
import com.supercilex.robotscouter.core.data.initRemoteConfig
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.shared.initUi
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.longToast

internal class RobotScouter : MultiDexApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return
        LeakCanary.install(this)

        _globalContext = this

        async { initIo() }.logFailures()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                vmBuilder.detectFileUriExposure()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vmBuilder.detectCleartextNetwork()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                vmBuilder.penaltyDeathOnFileUriExposure()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vmBuilder.detectContentUriWithoutPermission()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                vmBuilder.penaltyListener(mainExecutor, StrictMode.OnVmViolationListener {
                    longToast(it.message.orEmpty())
                })
            }
            StrictMode.setVmPolicy(vmBuilder.penaltyLog().build())

            StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyDeath()
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
