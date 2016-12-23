package com.supercilex.robotscouter.ui;

import android.app.Activity;
import android.support.annotation.StringRes;

import com.supercilex.robotscouter.util.BaseHelper;

public class ActivityHelper extends BaseHelper {
    private Activity mActivity;

    public ActivityHelper(Activity activity) {
        super();
        mActivity = activity;
    }

    public void showSnackbar(@StringRes int message, int length) {
        showSnackbar(mActivity, message, length);
    }

    public boolean isOffline() {
        return BaseHelper.isOffline(mActivity);
    }
}
