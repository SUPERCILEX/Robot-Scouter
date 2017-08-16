package com.supercilex.robotscouter.util

import android.content.Context
import android.net.ConnectivityManager
import com.supercilex.robotscouter.RobotScouter

private val connectivityManager by lazy {
    RobotScouter.INSTANCE.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}

fun isOffline() = connectivityManager.activeNetworkInfo?.isConnected == false
