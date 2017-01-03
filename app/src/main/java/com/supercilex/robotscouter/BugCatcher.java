package com.supercilex.robotscouter;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.supercilex.robotscouter.util.BaseHelper;

/**
 * A simple class for LeakCanary integration.
 */
public class BugCatcher extends Application {
    private RefWatcher mRefWatcher;

    public static RefWatcher getRefWatcher(Context context) {
        return ((BugCatcher) context.getApplicationContext()).mRefWatcher;
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

        // TODO see https://github.com/firebase/quickstart-android/issues/176#issuecomment-268841466
        if (/*BuildConfig.DEBUG*/false) {
            // Enable StrictMode
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                           .detectAll()
                                           .penaltyLog()
                                           .penaltyDeath()
                                           .build());
        }

        BaseHelper.resetJobDispatcher(this);
    }
}
