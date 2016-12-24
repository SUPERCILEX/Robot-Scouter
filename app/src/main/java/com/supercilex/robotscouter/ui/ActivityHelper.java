package com.supercilex.robotscouter.ui;

import android.app.Activity;
import android.support.annotation.StringRes;

import com.supercilex.robotscouter.util.BaseHelper;

public class ActivityHelper {
    private Activity mActivity;

    public ActivityHelper(Activity activity) {
        mActivity = activity;
    }

    public void showSnackbar(@StringRes int message, int length) {
        BaseHelper.showSnackbar(mActivity, message, length);
    }

    public boolean isOffline() {
        return BaseHelper.isOffline(mActivity);
    }
}
