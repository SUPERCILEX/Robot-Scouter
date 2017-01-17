package com.supercilex.robotscouter.data.client;

import android.content.Context;
import android.os.Build;

import com.supercilex.robotscouter.data.model.Team;

public class DownloadTeamDataJob {
    public static void start(Context context, Team team) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DownloadTeamDataJob21.start(context, team);
        } else {
            DownloadTeamDataJob14.start(team);
        }
    }
}
