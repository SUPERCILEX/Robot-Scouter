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
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }

        intent.component = intent.getParcelableExtra(KEY_COMPONENT)

        context.startActivity(intent)
    }

    companion object {
        private const val KEY_COMPONENT = "component"
        private const val KEY_CANCEL = "cancel"
        private const val KEY_NOTIFICATION_ID = "notification_id"

        fun getCancelIntent(context: Context, id: Int, intent: Intent): Intent =
                Intent(intent).putExtra(KEY_COMPONENT, intent.component)
                        .setComponent(ComponentName(context, NotificationForwarder::class.java))
                        .putExtra(KEY_NOTIFICATION_ID, id)
                        .putExtra(KEY_CANCEL, true)
    }
}
