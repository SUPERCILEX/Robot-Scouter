package com.supercilex.robotscouter.core.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.supercilex.robotscouter.core.RobotScouter
import org.jetbrains.anko.configuration
import org.jetbrains.anko.landscape

val colorPrimary by lazy {
    ContextCompat.getColor(RobotScouter, R.color.colorPrimary)
}
val colorPrimaryDark by lazy {
    ContextCompat.getColor(RobotScouter, R.color.colorPrimaryDark)
}

private val STORE_LISTING_URI = "market://details?id=com.supercilex.robotscouter".toUri()
private val LATEST_APK_URI =
        "https://github.com/SUPERCILEX/app-version-history/blob/master/Robot-Scouter/app-release.aab".toUri()

fun Context.isInTabletMode(): Boolean {
    val size: Int = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size == Configuration.SCREENLAYOUT_SIZE_LARGE && configuration.landscape ||
            size > Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun View.setOnLongClickListenerCompat(listener: View.OnLongClickListener) {
    setOnLongClickListener(listener)
    if (Build.VERSION.SDK_INT >= 23) {
        setOnContextClickListener { listener.onLongClick(this) }
    }
}

fun Activity.showStoreListing() = try {
    startActivity(Intent(Intent.ACTION_VIEW).setData(STORE_LISTING_URI))
} catch (e: ActivityNotFoundException) {
    startActivity(Intent(Intent.ACTION_VIEW).setData(LATEST_APK_URI))
}
