package com.supercilex.robotscouter.util;

import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public final class RemoteConfigHelper {
    private RemoteConfigHelper() {
        // no instance
    }

    public static Task<Void> fetchRemoteConfigValues(long defaultCacheExpiration) {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        long cacheExpiration = config.getInfo()
                .getConfigSettings()
                .isDeveloperModeEnabled() ? 0 : defaultCacheExpiration;
        return config.fetch(cacheExpiration);
    }
}
