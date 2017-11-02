package com.supercilex.robotscouter.util.ui

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.isSingleton
import org.jetbrains.anko.intentFor

const val EXPORT_GROUP = "export_group"
const val EXPORT_CHANNEL = "export"
const val EXPORT_IN_PROGRESS_CHANNEL = "export_in_progress"

/**
 * Android N+ limits notification updates to 10/sec and drops the rest. This limits notification
 * updates to 5/sec.
 */
const val SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS = 200L

val notificationManager: NotificationManager by lazy {
    RobotScouter.INSTANCE.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

fun initNotifications() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    notificationManager.createNotificationChannelGroups(
            listOf(NotificationChannelGroup(
                    EXPORT_GROUP,
                    RobotScouter.INSTANCE.getString(R.string.export_group_title)))
    )

    notificationManager.createNotificationChannels(
            listOf(getExportChannel(), getExportInProgressChannel())
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun getExportChannel(): NotificationChannel = NotificationChannel(
        EXPORT_CHANNEL,
        RobotScouter.INSTANCE.getString(R.string.export_channel_title),
        NotificationManager.IMPORTANCE_HIGH
).apply {
    group = EXPORT_GROUP
    description = RobotScouter.INSTANCE.getString(R.string.export_channel_desc)
    setShowBadge(true)
    enableVibration(true)
    enableLights(true)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getExportInProgressChannel(): NotificationChannel = NotificationChannel(
        EXPORT_IN_PROGRESS_CHANNEL,
        RobotScouter.INSTANCE.getString(R.string.export_progress_channel_title),
        NotificationManager.IMPORTANCE_LOW
).apply {
    group = EXPORT_GROUP
    description = RobotScouter.INSTANCE.getString(R.string.export_progress_channel_desc)
    setShowBadge(false)
    enableVibration(false)
    enableLights(false)
}

class NotificationIntentForwarder : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationManager.apply {
            val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, -1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activeNotifications.filter { it.id == notificationId }.let {
                    check(it.isSingleton) {
                        "Couldn't find unique notification id ($notificationId) in $activeNotifications"
                    }
                    it[0]
                }.notification.group?.let { key ->
                    val groupNotifications =
                            activeNotifications.filter { it.notification.group == key }
                    if (groupNotifications.size == 2) {
                        groupNotifications
                                .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
                                .forEach { cancel(it.id) }
                    }
                }
            }

            cancel(notificationId)
        }
        sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        startActivity(intent.getParcelableExtra(KEY_INTENT))
        finish()
    }

    companion object {
        private const val KEY_INTENT = "intent"
        private const val KEY_NOTIFICATION_ID = "notification_id"

        fun getCancelIntent(notificationId: Int, forwardedIntent: Intent): Intent =
                RobotScouter.INSTANCE.intentFor<NotificationIntentForwarder>(
                        KEY_INTENT to forwardedIntent,
                        KEY_NOTIFICATION_ID to notificationId
                )
    }
}
