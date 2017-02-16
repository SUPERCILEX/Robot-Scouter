package com.supercilex.robotscouter.util;

import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.concurrent.TimeUnit;

public final class RemoteConfigHelper {
    private RemoteConfigHelper() {
        // no instance
    }

    public static Task<Void> fetchRemoteConfigValues() {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        long cacheExpiration = config.getInfo()
                .getConfigSettings()
                .isDeveloperModeEnabled() ? 0 : TimeUnit.HOURS.toSeconds(12);
        return config.fetch(cacheExpiration);
    }
}
