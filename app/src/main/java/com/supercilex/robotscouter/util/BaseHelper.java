package com.supercilex.robotscouter.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.supercilex.robotscouter.R;

public final class BaseHelper {
    private static FirebaseDatabase sDatabase;
    private static FirebaseJobDispatcher sDispatcher;

    private BaseHelper() {
        // no instance
    }

    public static DatabaseReference getDatabase() {
        synchronized (BaseHelper.class) {
            if (sDatabase == null) {
                sDatabase = FirebaseDatabase.getInstance();
                sDatabase.setPersistenceEnabled(true);
            }
        }
        return sDatabase.getReference();
    }

    public static FirebaseJobDispatcher getDispatcher() {
        if (sDispatcher == null) {
            throw new IllegalStateException("FirebaseJobDispatcher was null");
        }
        return sDispatcher;
    }

    public static void resetJobDispatcher(Context context) {
        synchronized (BaseHelper.class) {
            sDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context.getApplicationContext()));
        }
    }

    public static Task<Void> fetchRemoteConfigValues(long defaultCacheExpiration) {
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        long cacheExpiration = config.getInfo()
                .getConfigSettings()
                .isDeveloperModeEnabled() ? 0L : defaultCacheExpiration;
        return config.fetch(cacheExpiration);
    }

    public static void runOnMainThread(Context context, Runnable runnable) {
        new Handler(context.getMainLooper()).post(runnable);
    }

    public static boolean isOffline(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return !(activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    public static void restoreRecyclerViewState(Bundle savedInstanceState,
                                                final RecyclerView.Adapter adapter,
                                                final RecyclerView.LayoutManager layoutManager) {
        if (savedInstanceState != null && adapter != null && layoutManager != null) {
            final Parcelable managerState = savedInstanceState.getParcelable(Constants.MANAGER_STATE);
            final int count = savedInstanceState.getInt(Constants.ITEM_COUNT);
            if (adapter.getItemCount() >= count) {
                layoutManager.onRestoreInstanceState(managerState);
            } else {
                adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        if (adapter.getItemCount() >= count) {
                            layoutManager.onRestoreInstanceState(managerState);
                            adapter.unregisterAdapterDataObserver(this);
                        }
                    }
                });
            }
        }
    }

    public static void saveRecyclerViewState(Bundle outState,
                                             RecyclerView.Adapter adapter,
                                             RecyclerView.LayoutManager layoutManager) {
        if (adapter != null) {
            outState.putParcelable(Constants.MANAGER_STATE, layoutManager.onSaveInstanceState());
            outState.putInt(Constants.ITEM_COUNT, adapter.getItemCount());
        }
    }

    private static Snackbar getSnackbar(View coordinatorLayoutView,
                                        @StringRes int message,
                                        int length) {
        // Can't use android.R.id.content or the snackbar will cover up the FAB and not be dismissible
        return Snackbar.make(coordinatorLayoutView, message, length);
    }

    public static Snackbar showSnackbar(View coordinatorLayoutView,
                                        @StringRes int message,
                                        int length) {
        Snackbar snackbar = getSnackbar(coordinatorLayoutView, message, length);
        snackbar.show();
        return snackbar;
    }

    public static Snackbar showSnackbar(View coordinatorLayoutView,
                                        @StringRes int message,
                                        int length,
                                        @StringRes int actionMessage,
                                        View.OnClickListener listener) {
        Snackbar snackbar = getSnackbar(coordinatorLayoutView, message, length);
        snackbar.setAction(actionMessage, listener).show();
        return snackbar;
    }

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
