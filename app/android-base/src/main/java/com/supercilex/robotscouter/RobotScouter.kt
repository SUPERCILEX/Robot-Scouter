package com.supercilex.robotscouter

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.UntaggedSocketViolation
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.play.core.splitcompat.SplitCompat
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core._globalContext
import com.supercilex.robotscouter.core.data.client.initWork
import com.supercilex.robotscouter.core.data.initAnalytics
import com.supercilex.robotscouter.core.data.initDatabase
import com.supercilex.robotscouter.core.data.initIo
import com.supercilex.robotscouter.core.data.initNotifications
import com.supercilex.robotscouter.core.data.initPrefs
import com.supercilex.robotscouter.core.data.initRemoteConfig
import com.supercilex.robotscouter.core.toast
import com.supercilex.robotscouter.shared.initUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class RobotScouter : MultiDexApplication(), Configuration.Provider {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        _globalContext = this

        GlobalScope.apply {
            // Prep slow init calls
            launch(Dispatchers.IO) { initIo() }
            launch(Dispatchers.IO) { initWork() }

            launch { Dispatchers.Main }
            launch { initBridges() }
            launch { Glide.get(RobotScouter) }
            launch { WorkManager.getInstance(RobotScouter) }

            launch { initRemoteConfig() }
            launch { initNotifications() }
        }

        // These calls must occur synchronously
        initAnalytics()
        initDatabase()
        initPrefs()
        initUi()

        if (BuildConfig.DEBUG) {
            // Purposefully put this after initialization since Google is terrible with disk I/O.
            val vmBuilder = StrictMode.VmPolicy.Builder().detectAll()
            if (Build.VERSION.SDK_INT >= 28) {
                vmBuilder.penaltyListener(mainExecutor, StrictMode.OnVmViolationListener {
                    if (
                        it !is UntaggedSocketViolation
                    ) toast("StrictMode VM violation! See logs for more details.")
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).onTrimMemory(level)
    }

    override fun getWorkManagerConfiguration() = Configuration.Builder().build()

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
