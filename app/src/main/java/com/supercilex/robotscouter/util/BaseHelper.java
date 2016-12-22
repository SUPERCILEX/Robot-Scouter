package com.supercilex.robotscouter.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.supercilex.robotscouter.R;

public class BaseHelper {
    private static FirebaseDatabase sDatabase;
    private static FirebaseAuth sAuth;
    private static FirebaseJobDispatcher sDispatcher;

    public static DatabaseReference getDatabase() {
        synchronized (BaseHelper.class) {
            if (sDatabase == null) {
                sDatabase = FirebaseDatabase.getInstance();
                sDatabase.setPersistenceEnabled(true);
            }
        }
        return sDatabase.getReference();
    }

    public static FirebaseAuth getAuth() {
        synchronized (BaseHelper.class) {
            if (sAuth == null) {
                sAuth = FirebaseAuth.getInstance();
            }
        }
        return sAuth;
    }

    public static FirebaseUser getUser() {
        return getAuth().getCurrentUser();
    }

    @Nullable
    public static String getUid() {
        return getUser() == null ? null : getUser().getUid();
    }

    public static boolean isSignedIn() {
        return getUser() != null;
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
        if (savedInstanceState != null) {
            final Parcelable managerState =
                    savedInstanceState.getParcelable(Constants.MANAGER_STATE);
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

    protected Snackbar getSnackbar(Activity activity, @StringRes int message, int length) {
        // Can't use android.R.id.content or the snackbar will cover up the FAB
        return Snackbar.make(activity.findViewById(R.id.root), message, length);
    }

    protected void showSnackbar(Activity activity, @StringRes int message, int length) {
        getSnackbar(activity, message, length).show();
    }

    protected void showSnackbar(Activity activity,
                                @StringRes int message,
                                int length,
                                @StringRes int actionMessage,
                                View.OnClickListener listener) {
        getSnackbar(activity, message, length).setAction(actionMessage, listener).show();
    }
}
