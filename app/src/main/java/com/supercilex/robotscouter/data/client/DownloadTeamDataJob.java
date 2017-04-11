package com.supercilex.robotscouter.data.client;

import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.JobHelper;

public final class DownloadTeamDataJob {
    private DownloadTeamDataJob() {
        throw new AssertionError("No instance for you!");
    }

    public static void start(Context context, TeamHelper teamHelper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobHelper.startInternetJob21(context,
                                         teamHelper,
                                         (int) teamHelper.getTeam().getNumberAsLong(),
                                         DownloadTeamDataJob21.class);
        } else {
            JobHelper.startInternetJob14(context,
                                         teamHelper,
                                         (int) teamHelper.getTeam().getNumberAsLong(),
                                         DownloadTeamDataJob14.class);
        }
    }

    public static void cancelAll(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            scheduler.cancelAll();
        } else {
            FirebaseJobDispatcher dispatcher =
                    new FirebaseJobDispatcher(new GooglePlayDriver(context.getApplicationContext()));
            dispatcher.cancelAll();
        }
    }
}
