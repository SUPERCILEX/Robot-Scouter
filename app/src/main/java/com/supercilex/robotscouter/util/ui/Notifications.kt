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
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.reportOrCancel
import org.jetbrains.anko.intentFor
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
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

val notificationManager: NotificationManager by lazy {
    RobotScouter.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

fun initNotifications() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    notificationManager.createNotificationChannelGroups(
            listOf(NotificationChannelGroup(
                    EXPORT_GROUP,
                    RobotScouter.getString(R.string.export_group_title)))
    )

    notificationManager.createNotificationChannels(
            listOf(getExportChannel(), getExportInProgressChannel())
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun getExportChannel(): NotificationChannel = NotificationChannel(
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

@RequiresApi(Build.VERSION_CODES.O)
fun getExportInProgressChannel(): NotificationChannel = NotificationChannel(
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
class FilteringNotificationManager : Runnable {
    private val lock = ReentrantReadWriteLock()
    private val notifications: MutableMap<Int, Notification> = LinkedHashMap()
    private val vips: Queue<Int> = LinkedList()

    private var updater: Future<*> by LateinitVal()
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

    /**
     * Starts the notification looper.
     *
     * @throws IllegalStateException if the looper has been stopped
     */
    fun start() {
        lock.read {
            check(!isStopped) { "Cannot start a previously stopped notification filter." }
        }

        updater = executor.scheduleWithFixedDelay(
                this,
                0,
                SAFE_NOTIFICATION_RATE_LIMIT_IN_MILLIS,
                TimeUnit.MILLISECONDS
        )
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
        updater.reportOrCancel(true)
        lock.write {
            vips.clear()
            notifications.clear()
            isStopped = true
        }
    }

    override fun run() {
        lock.write {
            vips.poll()?.let {
                notificationManager.notify(it, notifications.remove(it)!!)
            } ?: notifications.keys.firstOrNull()?.let {
                notificationManager.notify(it, notifications.remove(it)!!)
            }
        }

        lock.read {
            if (isStopped && notifications.isEmpty()) updater.reportOrCancel()
        }
    }

    private companion object {
        val executor = ScheduledThreadPoolExecutor(1)
    }
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
                RobotScouter.intentFor<NotificationIntentForwarder>(
                        KEY_INTENT to forwardedIntent,
                        KEY_NOTIFICATION_ID to notificationId
                )
    }
}
