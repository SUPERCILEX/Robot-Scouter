package com.supercilex.robotscouter.feature.autoscout

import android.arch.lifecycle.LifecycleService
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.AUTO_SCOUT_CHANNEL
import org.jetbrains.anko.intentFor

internal class AutoScoutService : LifecycleService() {
    private val connectionManager by lazy { ConnectionManager(this) }

    override fun onCreate() {
        super.onCreate()
        startForeground(
                R.string.auto_scout_channel_title,
                NotificationCompat.Builder(this, AUTO_SCOUT_CHANNEL)
                        .setSmallIcon(R.drawable.ic_auto_fix_white_24dp)
                        .setContentTitle(RobotScouter.getString(R.string.auto_scout_channel_title))
                        .setSubText("TODO")
                        .setContentText("TODO")
                        .setColor(ContextCompat.getColor(RobotScouter, R.color.colorPrimary))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent.action) {
            ACTION_ENSLAVE -> connectionManager.enslave()
            ACTION_FIND_MASTER -> connectionManager.findMaster()
            else -> error("Unknown action: ${intent.action}")
        }

        return START_NOT_STICKY
    }

    companion object {
        private const val ACTION_ENSLAVE = "action_enslave"
        private const val ACTION_FIND_MASTER = "action_find_master"

        fun enslave() {
            start(RobotScouter.intentFor<AutoScoutService>().setAction(ACTION_ENSLAVE))
        }

        fun findMaster() {
            start(RobotScouter.intentFor<AutoScoutService>().setAction(ACTION_FIND_MASTER))
        }

        private fun start(intent: Intent) =
                ContextCompat.startForegroundService(RobotScouter, intent)
    }
}
