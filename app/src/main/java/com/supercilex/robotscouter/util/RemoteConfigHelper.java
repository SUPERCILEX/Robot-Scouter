package com.supercilex.robotscouter.util;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.concurrent.TimeUnit;

public final class RemoteConfigHelper {
    private RemoteConfigHelper() {
        // no instance
    }

    public static Task<Void> fetchAndActivate() {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        long cacheExpiration = config.getInfo()
                .getConfigSettings()
                .isDeveloperModeEnabled() ? 0 : TimeUnit.HOURS.toSeconds(12);
        final TaskCompletionSource<Void> activate = new TaskCompletionSource<>();

        config.fetch(cacheExpiration).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                FirebaseRemoteConfig.getInstance().activateFetched();
                activate.setResult(null);
            }
        });

        return activate.getTask();
    }
}
