package com.supercilex.robotscouter.core

import android.content.Context
import android.net.ConnectivityManager

val isOnline get() = connectivityManager.activeNetworkInfo?.isConnected == true

val isOffline get() = !isOnline

private val connectivityManager by lazy {
    RobotScouter.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}
