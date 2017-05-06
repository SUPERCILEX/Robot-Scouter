package com.supercilex.robotscouter;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.supercilex.robotscouter.util.AnalyticsUtils;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.DatabaseHelper;
import com.supercilex.robotscouter.util.ViewUtils;

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

        Constants.init(this);
        DatabaseHelper.init(this);
        AnalyticsUtils.init(this);
        ViewUtils.init(this);

        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        config.setDefaults(R.xml.remote_config_defaults);
        config.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                                         .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                         .build());
    }
}
