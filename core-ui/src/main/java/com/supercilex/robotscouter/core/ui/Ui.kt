package com.supercilex.robotscouter.core.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import org.jetbrains.anko.configuration
import org.jetbrains.anko.landscape

fun Context.isInTabletMode(): Boolean {
    val size: Int = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return size == Configuration.SCREENLAYOUT_SIZE_LARGE && configuration.landscape
            || size > Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun View.setOnLongClickListenerCompat(listener: View.OnLongClickListener) {
    setOnLongClickListener(listener)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        setOnContextClickListener { listener.onLongClick(this) }
    }
}
