package com.supercilex.robotscouter.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;

import com.supercilex.robotscouter.R;

public enum CustomTabsUtils {;
    public static void launchUrl(Context context, Uri url) {
        getCustomTabsIntent(context).launchUrl(context, url);
    }

    private static CustomTabsIntent getCustomTabsIntent(Context context) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setShowTitle(true)
                .addDefaultShareMenuItem()
                .enableUrlBarHiding()
                .setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Add referrer intent
            customTabsIntent.intent.putExtra(
                    Intent.EXTRA_REFERRER,
                    Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + context.getPackageName()));
        }

        return customTabsIntent;
    }
}
