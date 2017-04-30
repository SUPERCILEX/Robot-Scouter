package com.supercilex.robotscouter.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public enum ConnectivityUtils {;
    public static boolean isOffline(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return !(activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }
}
