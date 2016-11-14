package com.supercilex.robotscouter.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
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
import com.supercilex.robotscouter.data.model.Team;

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

    public static String getUid() {
        return getUser().getUid();
    }

    public static FirebaseJobDispatcher getDispatcher() {
        if (sDispatcher == null) {
            throw new IllegalStateException("FirebaseJobDispatcher was null");
        }
        return sDispatcher;
    }

    public static FirebaseJobDispatcher getNewDispatcher(Context context) {
        synchronized (BaseHelper.class) {
            if (sDispatcher == null) {
                sDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
            }
        }
        return sDispatcher;
    }

    public static boolean isSignedIn() {
        return getUser() != null;
    }

    public static <T> String getTag(T clazz) {
        return clazz.getClass().getSimpleName();
    }

    public static void runOnMainThread(Context context, Runnable runnable) {
        new Handler(context.getMainLooper()).post(runnable);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Intent getTeamIntent(@NonNull Team team) {
        return new Intent().putExtra(Constants.INTENT_TEAM, team);
    }

    public static Team getTeam(Intent intent) {
        return (Team) Preconditions.checkNotNull(intent.getParcelableExtra(Constants.INTENT_TEAM),
                                                 "Team cannot be null");
    }

    public static Bundle getTeamBundle(@NonNull Team team) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.INTENT_TEAM, team);
        return bundle;
    }

    public static Team getTeam(Bundle arguments) {
        return (Team) Preconditions.checkNotNull(arguments.getParcelable(Constants.INTENT_TEAM),
                                                 "Team cannot be null");
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

    public Snackbar getSnackbar(Activity activity, @StringRes int message, int length) {
        return Snackbar.make(activity.findViewById(R.id.root), message, length);
    }

    public void showSnackbar(Activity activity, @StringRes int message, int length) {
        getSnackbar(activity, message, length).show();
    }

    public void showSnackbar(Activity activity,
                             @StringRes int message,
                             int length,
                             @StringRes int actionMessage,
                             View.OnClickListener listener) {
        getSnackbar(activity, message, length).setAction(actionMessage, listener).show();
    }
}
