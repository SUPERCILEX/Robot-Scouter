package com.supercilex.robotscouter.data.client;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class NotificationForwarder extends BroadcastReceiver {
    private static final String KEY_COMPONENT = "component";
    private static final String KEY_CANCEL = "cancel";
    private static final String KEY_NOTIFICATION_ID = "notification_id";

    public static Intent getCancelIntent(Context context, int id, Intent intent) {
        intent.putExtra(KEY_COMPONENT, intent.getComponent());

        return intent.setComponent(new ComponentName(context, NotificationForwarder.class))
                .putExtra(KEY_NOTIFICATION_ID, id)
                .putExtra(KEY_CANCEL, true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra(KEY_CANCEL, false)) {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(intent.getIntExtra(KEY_NOTIFICATION_ID, -1));
        }

        intent.setComponent(intent.getParcelableExtra(KEY_COMPONENT));

        context.startActivity(intent);
    }
}
