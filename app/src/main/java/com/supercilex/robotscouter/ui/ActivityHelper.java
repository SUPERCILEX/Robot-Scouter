package com.supercilex.robotscouter.ui;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.view.View;

import com.supercilex.robotscouter.data.model.Team;
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

    public void showSnackbar(@StringRes int message,
                             int length,
                             @StringRes int actionMessage,
                             View.OnClickListener listener) {
        showSnackbar(mActivity, message, length, actionMessage, listener);
    }

    public String getTag() {
        return getTag(mActivity);
    }

    public Team getTeam() {
        return BaseHelper.getTeam(mActivity.getIntent());
    }
}
