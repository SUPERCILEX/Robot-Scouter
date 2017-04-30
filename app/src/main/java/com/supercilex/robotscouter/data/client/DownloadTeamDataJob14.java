package com.supercilex.robotscouter.data.client;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.supercilex.robotscouter.data.remote.TbaDownloader;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.JobUtils;

public class DownloadTeamDataJob14 extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        TeamHelper oldTeamHelper = JobUtils.parseRawBundle(params.getExtras());
        TbaDownloader.load(oldTeamHelper.getTeam(), this)
                .addOnSuccessListener(newTeam -> {
                    oldTeamHelper.updateTeam(newTeam);
                    jobFinished(params, false);
                })
                .addOnFailureListener(e -> jobFinished(params, true));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return true;
    }
}
