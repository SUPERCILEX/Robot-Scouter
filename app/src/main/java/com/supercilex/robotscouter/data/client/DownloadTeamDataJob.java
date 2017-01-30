package com.supercilex.robotscouter.data.client;

import android.content.Context;
import android.os.Build;

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
}
