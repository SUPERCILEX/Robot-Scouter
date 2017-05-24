package com.supercilex.robotscouter.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

fun isOffline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
    return !(activeNetworkInfo?.isConnected ?: false)
}
