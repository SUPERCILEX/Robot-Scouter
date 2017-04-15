package com.supercilex.robotscouter.data.client;

import android.content.Context;
import android.os.Build;

import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.JobHelper;

public final class UploadTeamMediaJob {
    private UploadTeamMediaJob() {
        throw new AssertionError("No instance for you!");
    }

    public static void start(Context context, TeamHelper teamHelper) {
        int mediaHash = teamHelper.getTeam().getMedia().hashCode();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobHelper.startInternetJob21(context,
                                         teamHelper,
                                         mediaHash,
                                         UploadTeamMediaJob21.class);
        } else {
            JobHelper.startInternetJob14(context,
                                         teamHelper,
                                         mediaHash,
                                         UploadTeamMediaJob14.class);
        }
    }
}
