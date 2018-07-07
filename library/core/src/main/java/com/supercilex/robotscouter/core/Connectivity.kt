package com.supercilex.robotscouter.core

import android.net.ConnectivityManager
import androidx.core.content.getSystemService

val isOnline get() = connectivityManager.activeNetworkInfo?.isConnected == true

val isOffline get() = !isOnline

private val connectivityManager by lazy {
    checkNotNull(RobotScouter.getSystemService<ConnectivityManager>())
}
