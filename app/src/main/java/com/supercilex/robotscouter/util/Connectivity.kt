package com.supercilex.robotscouter.util

import android.content.Context
import android.net.ConnectivityManager
import com.supercilex.robotscouter.RobotScouter

val isOnline get() = !isOffline

val isOffline get() = connectivityManager.activeNetworkInfo?.isConnected == false

private val connectivityManager by lazy {
    RobotScouter.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}
