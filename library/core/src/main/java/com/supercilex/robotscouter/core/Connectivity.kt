package com.supercilex.robotscouter.core

import android.content.Context
import android.net.ConnectivityManager

val isOnline get() = !isOffline

val isOffline get() = connectivityManager.activeNetworkInfo?.isConnected == false

private val connectivityManager by lazy {
    RobotScouter.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}
