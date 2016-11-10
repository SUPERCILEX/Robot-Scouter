package com.supercilex.robotscouter.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
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
        if (sDatabase == null) {
            sDatabase = FirebaseDatabase.getInstance();
            sDatabase.setPersistenceEnabled(true);
        }
        return sDatabase.getReference();
    }

    public static FirebaseAuth getAuth() {
        if (sAuth == null) {
            sAuth = FirebaseAuth.getInstance();
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

    public static FirebaseJobDispatcher getDispatcher(Context context) {
        if (sDispatcher == null) {
            sDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
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
