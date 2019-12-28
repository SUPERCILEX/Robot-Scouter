package com.supercilex.robotscouter.core.data

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.supercilex.robotscouter.core.LateinitVal
import com.supercilex.robotscouter.core.RobotScouter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

const val EXPORT_GROUP = "export_group"
const val EXPORT_CHANNEL = "export"
const val EXPORT_IN_PROGRESS_CHANNEL = "export_in_progress"

/**
 * Android N+ limits notification updates to 10/sec and drops the rest. This limits notification
 * updates to 5/sec.
 */
const val SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS = 200L

val notificationManager by lazy { NotificationManagerCompat.from(RobotScouter) }
private val systemNotificationManager by lazy {
    checkNotNull(RobotScouter.getSystemService<NotificationManager>())
}

fun initNotifications() {
    logNotificationsEnabled(
            notificationManager.areNotificationsEnabled(),
            notificationManager.notificationChannels.associate {
                it.id to (it.importance != NotificationManagerCompat.IMPORTANCE_NONE)
            }
    )

    if (Build.VERSION.SDK_INT < 26) return

    notificationManager.createNotificationChannelGroups(
            listOf(NotificationChannelGroup(
                    EXPORT_GROUP,
                    RobotScouter.getString(R.string.export_group_title)))
    )

    notificationManager.createNotificationChannels(
            listOf(getExportChannel(), getExportInProgressChannel())
    )
}

@RequiresApi(26)
private fun getExportChannel(): NotificationChannel = NotificationChannel(
        EXPORT_CHANNEL,
        RobotScouter.getString(R.string.export_channel_title),
        NotificationManager.IMPORTANCE_HIGH
).apply {
    group = EXPORT_GROUP
    description = RobotScouter.getString(R.string.export_channel_desc)
    setShowBadge(true)
    enableVibration(true)
    enableLights(true)
}

@RequiresApi(26)
private fun getExportInProgressChannel(): NotificationChannel = NotificationChannel(
        EXPORT_IN_PROGRESS_CHANNEL,
        RobotScouter.getString(R.string.export_progress_channel_title),
        NotificationManager.IMPORTANCE_LOW
).apply {
    group = EXPORT_GROUP
    description = RobotScouter.getString(R.string.export_progress_channel_desc)
    setShowBadge(false)
    enableVibration(false)
    enableLights(false)
}

/**
 * Notification manager that throws away excess notifications in compliance with Android N's
 * rate limits.
 *
 * @see SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS
 */
class FilteringNotificationManager {
    private val lock = ReentrantReadWriteLock()
    private val notifications: MutableMap<Int, Notification> = LinkedHashMap()
    private val vips: Queue<Int> = LinkedList()

    private var processor: Job by LateinitVal()
    private var isStopped = false

    /**
     * Post a notification on the next available pass. Both the first and last (complete)
     * notifications are guaranteed to be posted.
     *
     * @see NotificationManager.notify
     */
    fun notify(id: Int, notification: Notification, isComplete: Boolean = false) {
        lock.read {
            check(!isStopped) { "Cannot notify a stopped notification filter." }
        }

        lock.write {
            if ((isComplete || !notifications.contains(id)) && !vips.contains(id)) {
                vips.add(id)
            }
            notifications[id] = notification
        }
    }

    fun cancel(id: Int) {
        lock.write {
            vips.remove(id)
            notifications.remove(id)
        }
    }

    /**
     * Starts the notification looper.
     *
     * @throws IllegalStateException if the looper has been stopped
     */
    fun start() {
        lock.read {
            check(!isStopped) { "Cannot start a previously stopped notification filter." }
        }

        processor = GlobalScope.launch {
            while (isActive) {
                processQueue()
                delay(SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS)
            }
        }
    }

    fun isStopped(): Boolean = lock.read { isStopped }

    /**
     * Stops the looper as soon as possible. All posted notifications will be processed before
     * stopping.
     *
     * @throws IllegalStateException if there are incomplete notifications
     */
    fun stop() = lock.write {
        check(vips.size == notifications.size && vips.all { notifications[it] != null }) {
            "Cannot stop a notification filter with incomplete notifications."
        }

        isStopped = true
    }

    fun stopNow() {
        processor.cancel()
        lock.write {
            vips.clear()
            notifications.clear()
            isStopped = true
        }
    }

    private fun processQueue() {
        lock.write {
            (vips.poll() ?: notifications.keys.firstOrNull())?.let {
                notificationManager.notify(it, checkNotNull(notifications.remove(it)))
            }
        }

        lock.read {
            if (isStopped && notifications.isEmpty()) processor.cancel()
        }
    }
}

class NotificationIntentForwarder : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemNotificationManager.apply {
            val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, -1)

            if (Build.VERSION.SDK_INT >= 23) {
                // Cancel group notification if there will only be one real notification left
                activeNotifications.singleOrNull {
                    it.id == notificationId
                }?.notification?.group?.let { key ->
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

        fun getCancelIntent(
                notificationId: Int,
                forwardedIntent: Intent
        ): Intent = Intent(RobotScouter, NotificationIntentForwarder::class.java)
                .putExtra(KEY_INTENT, forwardedIntent)
                .putExtra(KEY_NOTIFICATION_ID, notificationId)
    }
}
