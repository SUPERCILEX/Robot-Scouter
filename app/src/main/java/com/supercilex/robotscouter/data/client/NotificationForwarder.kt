package com.supercilex.robotscouter.data.client

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class NotificationForwarder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(KEY_CANCEL, false)) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(intent.getIntExtra(KEY_NOTIFICATION_ID, -1))
        }

        intent.component = intent.getParcelableExtra<ComponentName>(KEY_COMPONENT)

        context.startActivity(intent)
    }

    companion object {
        private val KEY_COMPONENT = "component"
        private val KEY_CANCEL = "cancel"
        private val KEY_NOTIFICATION_ID = "notification_id"

        fun getCancelIntent(context: Context, id: Int, intent: Intent): Intent =
                intent.putExtra(KEY_COMPONENT, intent.component)
                        .setComponent(ComponentName(context, NotificationForwarder::class.java))
                        .putExtra(KEY_NOTIFICATION_ID, id)
                        .putExtra(KEY_CANCEL, true)
    }
}
