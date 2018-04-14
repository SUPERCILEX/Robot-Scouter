package com.supercilex.robotscouter.core

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import android.support.v4.app.ActivityManagerCompat
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

val fullVersionName: String by lazy {
    // Get it from the package manager instead of the BuildConfig to support injected version names
    // from the automated build system.
    RobotScouter.packageManager.getPackageInfo(RobotScouter.packageName, 0).versionName
}
val fullVersionCode: Int by lazy {
    // See fullVersionName
    RobotScouter.packageManager.getPackageInfo(RobotScouter.packageName, 0).versionCode
}
val providerAuthority: String by lazy { "${RobotScouter.packageName}.provider" }
val refWatcher: RefWatcher by lazy { LeakCanary.install(RobotScouter) }
val isLowRamDevice: Boolean by lazy {
    ActivityManagerCompat.isLowRamDevice(
            RobotScouter.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
}
val isInTestMode: Boolean by lazy {
    Settings.System.getString(RobotScouter.contentResolver, "firebase.test.lab") == "true"
}
