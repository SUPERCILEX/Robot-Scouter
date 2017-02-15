package com.supercilex.robotscouter.data.client;

import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.supercilex.robotscouter.data.util.TeamHelper;

public final class DownloadTeamDataJob {
    private DownloadTeamDataJob() {
        // no instance
    }

    public static void start(Context context, TeamHelper teamHelper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DownloadTeamDataJob21.start(context, teamHelper);
        } else {
            DownloadTeamDataJob14.start(context, teamHelper);
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
