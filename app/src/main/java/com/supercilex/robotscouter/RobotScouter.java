package com.supercilex.robotscouter;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.supercilex.robotscouter.util.BaseHelper;

/**
 * A simple class for LeakCanary integration.
 */
public class RobotScouter extends Application {
    private RefWatcher mRefWatcher;

    public static RefWatcher getRefWatcher(Context context) {
        return ((RobotScouter) context.getApplicationContext()).mRefWatcher;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        mRefWatcher = LeakCanary.install(this);

        BaseHelper.resetJobDispatcher(this);
    }
}
