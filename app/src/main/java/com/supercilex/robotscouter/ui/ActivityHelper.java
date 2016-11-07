package com.supercilex.robotscouter.ui;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.BaseHelper;

public class ActivityHelper extends BaseHelper {
    private Activity mActivity;

    public ActivityHelper(Activity activity) {
        mActivity = activity;
    }

    public Snackbar getSnackbar(@StringRes int message, int length) {
        return Snackbar.make(mActivity.findViewById(R.id.root), message, length);
    }

    public void showSnackbar(@StringRes int message, int length) {
        getSnackbar(message, length).show();
    }

    public void showSnackbar(@StringRes int message,
                             int length,
                             @StringRes int actionMessage,
                             View.OnClickListener listener) {
        getSnackbar(message, length).setAction(actionMessage, listener).show();
    }

    public String getTag() {
        return getTag(mActivity);
    }
}
