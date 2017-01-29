package com.supercilex.robotscouter;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.supercilex.robotscouter.util.BaseHelper;

public class RobotScouter extends MultiDexApplication {
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

        // TODO see https://github.com/firebase/quickstart-android/issues/176#issuecomment-268841466
//        if (BuildConfig.DEBUG) {
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                                           .detectAll()
//                                           .penaltyLog()
//                                           .penaltyDeath()
//                                           .build());
//        }
        // END QUALITY CHECKS

        BaseHelper.resetJobDispatcher(this);

        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        config.setDefaults(R.xml.remote_config_defaults);
        config.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                                         .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                         .build());
    }
}
