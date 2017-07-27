package com.supercilex.robotscouter.util

import android.content.Context
import android.net.ConnectivityManager
import kotlin.properties.Delegates

private var connectivityManager by Delegates.notNull<ConnectivityManager>()

fun initConnectivity(context: Context) {
    connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}

fun isOffline(): Boolean = !(connectivityManager.activeNetworkInfo?.isConnected ?: false)
