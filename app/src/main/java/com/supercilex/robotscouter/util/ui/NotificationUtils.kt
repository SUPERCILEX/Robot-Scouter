package com.supercilex.robotscouter.util.ui

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import java.util.Arrays

const val EXPORT_GROUP = "export_group"
const val EXPORT_CHANNEL = "export"
const val EXPORT_IN_PROGRESS_CHANNEL = "export_in_progress"

fun initNotifications() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    RobotScouter.INSTANCE.getSystemService(NotificationManager::class.java).apply {
        createNotificationChannelGroups(Arrays.asList(
                NotificationChannelGroup(
                        EXPORT_GROUP, RobotScouter.INSTANCE.getString(R.string.export_group_name))))

        createNotificationChannels(Arrays.asList(
                getExportChannel(),
                getExportInProgressChannel()))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getExportChannel(): NotificationChannel = NotificationChannel(
        EXPORT_CHANNEL,
        RobotScouter.INSTANCE.getString(R.string.export_channel_name),
        NotificationManager.IMPORTANCE_HIGH).apply {
    group = EXPORT_GROUP
    description = RobotScouter.INSTANCE.getString(R.string.export_channel_description)
    setShowBadge(true)
    enableVibration(true)
    enableLights(true)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getExportInProgressChannel(): NotificationChannel = NotificationChannel(
        EXPORT_IN_PROGRESS_CHANNEL,
        RobotScouter.INSTANCE.getString(R.string.export_in_progress_channel_name),
        NotificationManager.IMPORTANCE_LOW).apply {
    group = EXPORT_GROUP
    description = RobotScouter.INSTANCE.getString(R.string.export_in_progress_channel_description)
    setShowBadge(false)
    enableVibration(false)
    enableLights(false)
}
