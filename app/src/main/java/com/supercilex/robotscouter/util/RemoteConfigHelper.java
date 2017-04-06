package com.supercilex.robotscouter.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.concurrent.TimeUnit;

public final class RemoteConfigHelper {
    private RemoteConfigHelper() {
        throw new AssertionError("No instance for you!");
    }

    public static Task<Void> fetchAndActivate() {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        long cacheExpiration = config.getInfo()
                .getConfigSettings()
                .isDeveloperModeEnabled() ? 0 : TimeUnit.HOURS.toSeconds(12);
        TaskCompletionSource<Void> activate = new TaskCompletionSource<>();

        config.fetch(cacheExpiration).addOnCompleteListener(task -> {
            FirebaseRemoteConfig.getInstance().activateFetched();
            activate.setResult(null);
        });

        return activate.getTask();
    }
}
